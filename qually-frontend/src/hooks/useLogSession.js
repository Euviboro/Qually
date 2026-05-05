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

  const isAccountabilityMode = protocol?.auditLogicType === "ACCOUNTABILITY";

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
        const subattributes = Object.fromEntries(
          (q.subattributes ?? []).map((s) => {
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
        // Clear subattribute selections whenever the answer changes away from NO
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

    // ACCOUNTABILITY: every NO answer must have the accountability subattribute selected
    if (isAccountabilityMode) {
      const missingAccountability = questions
        .filter((q) => answers[q.questionId]?.answer === ANSWERS.NO)
        .filter((q) => {
          const accountabilitySub = (q.subattributes ?? []).find(
            (s) => s.isAccountabilitySubattribute
          );
          if (!accountabilitySub) return false; // no accountability sub on this question
          const selectedOptionId = answers[q.questionId]?.subattributes?.[accountabilitySub.subattributeId];
          return selectedOptionId == null; // not selected
        })
        .map((q) => {
          const idx = questions.indexOf(q);
          return idx + 1;
        });

      if (missingAccountability.length > 0) {
        missing.push(
          `Accountability (required for No answers on question${missingAccountability.length > 1 ? "s" : ""} ${missingAccountability.join(", ")})`
        );
      }
    }

    return missing;
  }, [meta, questions, answers, commentsRequired, isAccountabilityMode]);

  const canSubmit = missingFields.length === 0 && totalCount > 0;

  // ── Persistence ────────────────────────────────────────────

  const [saving,      setSaving]      = useState(false);
  const [saveError,   setSaveError]   = useState(null);
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
    isAccountabilityMode,
    answeredCount, totalCount, isComplete,
    canDraft, canSubmit, missingFields,
    commentsRequired,
    saving, saveError, sessionId, savedStatus,
    saveDraft, submitSession,
  };
}