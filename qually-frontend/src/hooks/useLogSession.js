/** @module hooks/useLogSession */

import { useState, useMemo, useCallback, useEffect } from "react";
import { createSession, updateSession, submitBulkResponses } from "../api/sessions";
import { ANSWERS } from "../constants";

/**
 * Manages all state for the Log Session page — both new sessions and resumed drafts.
 *
 * @param {import('../api/protocols').AuditProtocolResponseDTO|null} protocol
 * @param {number|null} auditorUserId - The logged-in user's ID (from AuthContext).
 * @param {import('../api/sessions').SessionResumeDTO|null} [existingSession]
 *   When provided, pre-populates all form fields and answers from the draft.
 *   The session ID is also seeded so the next save updates the existing record
 *   rather than creating a new one.
 */
export function useLogSession(protocol, auditorUserId, existingSession = null) {

  // ── Meta state ─────────────────────────────────────────────

  const [meta, setMetaState] = useState(() => ({
    interactionId:       existingSession?.interactionId       ?? "",
    lobId:               existingSession?.lobId               ?? null,
    memberAuditedUserId: existingSession?.memberAuditedUserId ?? null,
    comments:            existingSession?.comments            ?? "",
  }));

  const setMeta = useCallback((field, value) => {
    setMetaState((prev) => ({ ...prev, [field]: value }));
  }, []);

  // When existingSession changes (e.g. async load completes), sync meta
  useEffect(() => {
    if (!existingSession) return;
    setMetaState({
      interactionId:       existingSession.interactionId       ?? "",
      lobId:               existingSession.lobId               ?? null,
      memberAuditedUserId: existingSession.memberAuditedUserId ?? null,
      comments:            existingSession.comments            ?? "",
    });
  }, [existingSession]);

  // ── Answer state ───────────────────────────────────────────

  /**
   * Builds the initial answers map from the protocol's questions.
   * When resuming, pre-populates answers and subattribute selections from
   * the previously saved responses.
   */
  const initialAnswers = useMemo(() => {
    if (!protocol?.auditQuestions) return {};

    // Index resume responses by questionId for O(1) lookup
    const resumeByQuestion = new Map(
      (existingSession?.responses ?? []).map((r) => [r.questionId, r])
    );

    return Object.fromEntries(
      protocol.auditQuestions.map((q) => {
        const saved = resumeByQuestion.get(q.questionId);

        // Build subattribute selections from the saved option IDs.
        // The protocol gives us subattribute IDs; we need to map each
        // subattribute to its previously selected option (if any).
        const subattributes = Object.fromEntries(
          (q.subattributes ?? []).map((s) => {
            // Find whether any of the saved option IDs belongs to this subattribute
            const matchingOptionId = saved?.subattributeOptionIds?.find((optId) =>
              s.subattributeOptions?.some((o) => o.subattributeOptionId === optId)
            ) ?? null;
            return [s.subattributeId, matchingOptionId];
          })
        );

        return [
          q.questionId,
          {
            answer:        saved?.questionAnswer ?? null,
            subattributes,
          },
        ];
      })
    );
  }, [protocol, existingSession]);

  const [answers, setAnswers] = useState(initialAnswers);

  // Re-sync answers when existingSession loads after initial render
  useEffect(() => {
    if (existingSession) setAnswers(initialAnswers);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [existingSession]);

  const setAnswer = useCallback((questionId, answer) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        answer,
        subattributes: answer !== ANSWERS.NO
          ? Object.fromEntries(
              Object.keys(prev[questionId]?.subattributes ?? {}).map((k) => [k, null])
            )
          : prev[questionId]?.subattributes ?? {},
      },
    }));
  }, []);

  const setSubattributeAnswer = useCallback((questionId, subattributeId, optionId) => {
    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        subattributes: { ...prev[questionId]?.subattributes, [subattributeId]: optionId },
      },
    }));
  }, []);

  const isQuestionExpanded = useCallback(
    (questionId) => answers[questionId]?.answer === ANSWERS.NO,
    [answers]
  );

  // ── Derived state ──────────────────────────────────────────

  const questions     = protocol?.auditQuestions ?? [];
  const totalCount    = questions.length;
  const answeredCount = Object.values(answers).filter((a) => a.answer !== null).length;
  const isComplete    = totalCount > 0 && answeredCount === totalCount;
  const canDraft      = answeredCount > 0;

  const hasAnyNo = useMemo(
    () => Object.values(answers).some((a) => a.answer === ANSWERS.NO),
    [answers]
  );
  const commentsRequired = hasAnyNo;

  const missingFields = useMemo(() => {
    const missing = [];
    if (!meta.interactionId.trim()) missing.push("Interaction ID");
    if (!meta.lobId)                missing.push("LOB");
    if (!meta.memberAuditedUserId)  missing.push("Member Audited");
    const unansweredNums = questions
      .map((q, i) => (answers[q.questionId]?.answer == null ? i + 1 : null))
      .filter(Boolean);
    if (unansweredNums.length > 0) missing.push(`Questions (${unansweredNums.join(", ")})`);
    if (commentsRequired && !meta.comments.trim()) {
      missing.push("Comments (required when any answer is No)");
    }
    return missing;
  }, [meta, questions, answers, commentsRequired]);

  const canSubmit = missingFields.length === 0 && totalCount > 0;

  // ── Persistence ────────────────────────────────────────────

  const [saving,      setSaving]      = useState(false);
  const [saveError,   setSaveError]   = useState(null);
  // Seed sessionId from the existing draft so the next save is an update
  const [sessionId,   setSessionId]   = useState(existingSession?.sessionId ?? null);
  const [savedStatus, setSavedStatus] = useState(null);

  const buildResponseItems = useCallback(() =>
    Object.entries(answers)
      .filter(([, a]) => a.answer !== null)
      .map(([questionId, a]) => {
        const subattributeAnswers = Object.values(a.subattributes ?? {})
          .filter((optionId) => optionId !== null)
          .map((optionId) => ({ subattributeOptionId: optionId }));
        return {
          questionId:     parseInt(questionId, 10),
          questionAnswer: a.answer,
          subattributeAnswers: subattributeAnswers.length > 0
            ? subattributeAnswers
            : undefined,
        };
      }),
    [answers]
  );

  const persist = useCallback(async (status) => {
    setSaving(true);
    setSaveError(null);
    try {
      let sid = sessionId;
      if (!sid) {
        const created = await createSession({
          protocolId:          protocol.protocolId,
          interactionId:       meta.interactionId.trim() || `DRAFT-${Date.now()}`,
          auditorUserId,
          memberAuditedUserId: meta.memberAuditedUserId,
          lobId:               meta.lobId,
          comments:            meta.comments.trim() || undefined,
          auditStatus:         status,
        });
        sid = created.sessionId;
        setSessionId(sid);
      } else {
        await updateSession(sid, {
          auditStatus: status,
          comments:    meta.comments.trim() || undefined,
        });
      }
      const items = buildResponseItems();
      if (items.length > 0) {
        await submitBulkResponses({ sessionId: sid, responses: items });
      }
      setSavedStatus(status);
      return sid;
    } catch (err) {
      setSaveError(err.message ?? "Something went wrong. Please try again.");
      return null;
    } finally {
      setSaving(false);
    }
  }, [sessionId, protocol, meta, auditorUserId, buildResponseItems]);

  const saveDraft     = useCallback(() => persist("DRAFT"),     [persist]);
  const submitSession = useCallback(() => persist("COMPLETED"), [persist]);

  return {
    meta, setMeta,
    answers, setAnswer, setSubattributeAnswer,
    isQuestionExpanded,
    answeredCount, totalCount, isComplete,
    canDraft, canSubmit, missingFields,
    commentsRequired,
    saving, saveError, sessionId, savedStatus,
    saveDraft, submitSession,
  };
}