/** @module hooks/useLogSession */

import { useState, useMemo, useCallback } from "react";
import { createSession, updateSession, submitBulkResponses } from "../api/sessions";

/**
 * @typedef {Object} QuestionAnswer
 * @property {"YES"|"NO"|null} answer
 * @property {Record<number, number|null>} subattributes - subattributeId → optionId | null
 */

/**
 * Top-level session metadata collected on the form.
 *
 * <p>Changes from the previous version:</p>
 * <ul>
 *   <li>{@code logicType} removed — scoring strategy is fixed on the protocol.</li>
 *   <li>{@code memberAudited} added — the name/ID of the person being audited.</li>
 *   <li>{@code auditorEmail} replaced by {@code auditorUserId} (number|null) —
 *       the auditor FK now references {@code users.user_id}.</li>
 * </ul>
 *
 * @typedef {Object} SessionMeta
 * @property {string}      interactionId
 * @property {string}      memberAudited
 * @property {number|null} auditorUserId
 * @property {string}      comments
 */

/**
 * @typedef {Object} UseLogSessionResult
 * @property {SessionMeta} meta
 * @property {(field: keyof SessionMeta, value: string|number|null) => void} setMeta
 * @property {Record<number, QuestionAnswer>} answers
 * @property {(questionId: number, answer: "YES"|"NO") => void} setAnswer
 * @property {(questionId: number, subattributeId: number, optionId: number) => void} setSubattributeAnswer
 * @property {(questionId: number) => boolean} isQuestionExpanded
 * @property {number}  answeredCount
 * @property {number}  totalCount
 * @property {boolean} isComplete
 * @property {boolean} canSave
 * @property {boolean} saving
 * @property {string|null} saveError
 * @property {number|null} sessionId
 * @property {"DRAFT"|"COMPLETED"|null} savedStatus
 * @property {() => Promise<void>} saveDraft
 * @property {() => Promise<void>} submitSession
 */

/**
 * Manages all state for the Log Session page.
 *
 * @param {import('../api/protocols').AuditProtocolResponseDTO|null} protocol
 * @returns {UseLogSessionResult}
 */
export function useLogSession(protocol) {
  // ── Session metadata ───────────────────────────────────────

  const [meta, setMetaState] = useState({
    interactionId: "",
    memberAudited: "",
    auditorUserId: null,
    comments:      "",
  });

  /**
   * Updates a single metadata field.
   *
   * @param {keyof SessionMeta} field
   * @param {string|number|null} value
   */
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
        subattributes: answer === "YES"
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
    (questionId) => answers[questionId]?.answer === "NO",
    [answers]
  );

  // ── Derived state ──────────────────────────────────────────

  const totalCount    = protocol?.auditQuestions?.length ?? 0;
  const answeredCount = Object.values(answers).filter((a) => a.answer !== null).length;
  const isComplete    = totalCount > 0 && answeredCount === totalCount;

  /**
   * Required metadata: interactionId, memberAudited, and a selected auditor.
   * Does NOT require all questions to be answered (that's `isComplete`).
   */
  const canSave = Boolean(
    meta.interactionId.trim() &&
    meta.memberAudited.trim() &&
    meta.auditorUserId !== null
  );

  // ── Persistence ────────────────────────────────────────────

  const [saving,      setSaving]      = useState(false);
  const [saveError,   setSaveError]   = useState(null);
  const [sessionId,   setSessionId]   = useState(null);
  const [savedStatus, setSavedStatus] = useState(null);

  const buildResponseItems = useCallback(() =>
    Object.entries(answers)
      .filter(([, a]) => a.answer !== null)
      .map(([questionId, a]) => ({
        questionId:     parseInt(questionId, 10),
        questionAnswer: a.answer,
      })),
    [answers]
  );

  const persist = useCallback(async (status) => {
    setSaving(true);
    setSaveError(null);
    try {
      let sid = sessionId;
      if (!sid) {
        const created = await createSession({
          protocolId:    protocol.protocolId,
          interactionId: meta.interactionId.trim(),
          auditorUserId: meta.auditorUserId,
          memberAudited: meta.memberAudited.trim(),
          comments:      meta.comments.trim() || undefined,
          auditStatus:   status,
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
  }, [sessionId, protocol, meta, buildResponseItems]);

  const saveDraft    = useCallback(() => persist("DRAFT"),     [persist]);
  const submitSession = useCallback(() => persist("COMPLETED"), [persist]);

  return {
    meta, setMeta,
    answers, setAnswer, setSubattributeAnswer,
    isQuestionExpanded,
    answeredCount, totalCount, isComplete,
    canSave, saving, saveError, sessionId, savedStatus,
    saveDraft, submitSession,
  };
}
