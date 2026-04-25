/** @module hooks/useLogSession */

import { useState, useMemo, useCallback } from "react";
import { createSession, updateSession, submitBulkResponses } from "../api/sessions";
import { ANSWERS } from "../constants";

export function useLogSession(protocol) {
  const [meta, setMetaState] = useState({
    interactionId:       "",
    lobId:               null,
    auditorUserId:       null,
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
        // Clear subattribute selections when switching away from NO
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
        subattributes: {
          ...prev[questionId]?.subattributes,
          [subattributeId]: optionId,
        },
      },
    }));
  }, []);

  // Subattribute panel expands only for NO answers
  const isQuestionExpanded = useCallback(
    (questionId) => answers[questionId]?.answer === ANSWERS.NO,
    [answers]
  );

  // ── Derived state ──────────────────────────────────────────

  const totalCount    = protocol?.auditQuestions?.length ?? 0;
  const answeredCount = Object.values(answers).filter((a) => a.answer !== null).length;
  const isComplete    = totalCount > 0 && answeredCount === totalCount;

  const canSave = Boolean(
    meta.interactionId.trim() &&
    meta.lobId !== null &&
    meta.auditorUserId !== null &&
    meta.memberAuditedUserId !== null
  );

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
          interactionId:       meta.interactionId.trim(),
          auditorUserId:       meta.auditorUserId,
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
  }, [sessionId, protocol, meta, buildResponseItems]);

  const saveDraft     = useCallback(() => persist("DRAFT"),     [persist]);
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
