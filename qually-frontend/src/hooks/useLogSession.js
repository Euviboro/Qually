/** @module hooks/useLogSession */

import { useState, useMemo, useCallback } from "react";
import { createSession, updateSession, submitBulkResponses } from "../api/sessions";
import { ANSWERS } from "../constants";

/**
 * @typedef {Object} SessionMeta
 * @property {string}      interactionId
 * @property {number|null} lobId
 * @property {number|null} memberAuditedUserId
 * @property {string}      comments
 */

/**
 * Manages all state for the Log Session page.
 *
 * Changes from the previous version:
 * - `auditorUserId` removed from meta — the logged-in user IS the auditor.
 *   The caller passes `auditorUserId` as a parameter so the hook can include
 *   it in the session payload without exposing it as editable state.
 * - `canDraft` added — true when at least one question has been answered,
 *   regardless of whether session details are filled. Allows saving progress
 *   before all metadata is known.
 * - `missingFields` computed and returned — a human-readable list of what
 *   still needs to be completed before submission. Used by the page to render
 *   the "You are missing: …" warning when the user attempts to submit.
 * - Comments are mandatory when any current answer is NO. This is reflected
 *   in `missingFields` and in `canSubmit`.
 *
 * @param {import('../api/protocols').AuditProtocolResponseDTO|null} protocol
 * @param {number|null} auditorUserId - The logged-in user's ID (from AuthContext).
 */
export function useLogSession(protocol, auditorUserId) {

  const [meta, setMetaState] = useState({
    interactionId:       "",
    lobId:               null,
    memberAuditedUserId: null,
    comments:            "",
  });

  const setMeta = useCallback((field, value) => {
    setMetaState((prev) => ({ ...prev, [field]: value }));
  }, []);

  // ── Per-question answers ───────────────────────────────────

  const initialAnswers = useMemo(() => {
    if (!protocol?.auditQuestions) return {};
    return Object.fromEntries(
      protocol.auditQuestions.map((q) => [
        q.questionId,
        {
          answer: null,
          subattributes: Object.fromEntries(
            (q.subattributes ?? []).map((s) => [s.subattributeId, null])
          ),
        },
      ])
    );
  }, [protocol]);

  const [answers, setAnswers] = useState(initialAnswers);

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

  /** True as soon as any question has been answered — enables Save Draft. */
  const canDraft = answeredCount > 0;

  /**
   * Checks whether comments are currently required.
   * Comments are mandatory when at least one question is answered NO.
   */
  const hasAnyNo = useMemo(
    () => Object.values(answers).some((a) => a.answer === ANSWERS.NO),
    [answers]
  );

  const commentsRequired = hasAnyNo;

  /**
   * Builds the human-readable list of incomplete fields.
   * Used to render the warning on failed submit attempts.
   * Includes question numbers (1-based) for unanswered questions.
   */
  const missingFields = useMemo(() => {
    const missing = [];

    if (!meta.interactionId.trim()) missing.push("Interaction ID");
    if (!meta.lobId)                missing.push("LOB");
    if (!meta.memberAuditedUserId)  missing.push("Member Audited");

    const unansweredNums = questions
      .map((q, i) => (answers[q.questionId]?.answer == null ? i + 1 : null))
      .filter(Boolean);
    if (unansweredNums.length > 0) {
      missing.push(`Questions (${unansweredNums.join(", ")})`);
    }

    if (commentsRequired && !meta.comments.trim()) {
      missing.push("Comments (required when any answer is No)");
    }

    return missing;
  }, [meta, questions, answers, commentsRequired]);

  /** All fields complete — enables Submit Session. */
  const canSubmit = missingFields.length === 0 && totalCount > 0;

  // ── Persistence ────────────────────────────────────────────

  const [saving,      setSaving]      = useState(false);
  const [saveError,   setSaveError]   = useState(null);
  const [sessionId,   setSessionId]   = useState(null);
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
          auditorUserId,                        // from AuthContext, not meta
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
    } catch (err) {
      setSaveError(err.message ?? "Something went wrong. Please try again.");
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