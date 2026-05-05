import { Btn } from "../../components/ui/Btn";
import { Label } from "../../components/ui/Label";
import { SectionCard } from "../../components/ui/SectionCard";
import { CategorySelector, COPC_CATEGORIES } from "../../components/ui/CategorySelector";
import { AttributeEditor } from "./AttributeEditor";
import { EMPTY_ATTRIBUTE } from "../../hooks/useNewProtocol";
import { COPC_CATEGORY_META } from "../../constants";

/**
 * @param {Object}  props
 * @param {Object}  props.question
 * @param {number}  props.index
 * @param {Function} props.onChange
 * @param {Function} props.onRemove
 * @param {boolean} props.canRemove
 * @param {"STANDARD"|"ACCOUNTABILITY"|null} props.auditLogicType
 */
export function QuestionCard({ question, index, onChange, onRemove, canRemove, auditLogicType }) {
  const isConfirmed  = question.confirmed;
  const categoryMeta = COPC_CATEGORY_META[question.category];
  const isAccountabilityMode = auditLogicType === "ACCOUNTABILITY";

  /**
   * When an attribute is toggled as the accountability subattribute,
   * clear the flag on all other siblings so at most one is marked.
   */
  const handleAttributeChange = (attrId, updated) => {
    let siblings = question.subattributes.map((a) =>
      a.id === attrId ? updated : a
    );

    // If the changed attribute just became the accountability one,
    // clear it on every other sibling to enforce the "at most one" rule.
    if (updated.isAccountabilitySubattribute) {
      siblings = siblings.map((a) =>
        a.id === attrId ? a : { ...a, isAccountabilitySubattribute: false }
      );
    }

    onChange({ ...question, subattributes: siblings });
  };

  return (
    <SectionCard className={`transition-all duration-300 ${isConfirmed ? "border-border-sec" : "border-lsg-blue ring-4 ring-lsg-blue/5"}`}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2.5">
          <span
            className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold font-mono
              ${isConfirmed ? "bg-bg-tertiary text-text-ter" : "bg-bg-accent text-lsg-blue"}`}
          >
            {index + 1}
          </span>
          <span className={`text-sm font-bold uppercase tracking-tight ${isConfirmed ? "text-text-ter" : "text-lsg-blue"}`}>
            {isConfirmed ? "Question confirmed" : "New question"}
          </span>
          {isConfirmed && categoryMeta && (
            <span
              style={{
                background: `var(${categoryMeta.bgVar})`,
                color: `var(${categoryMeta.textVar})`,
              }}
              className="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase"
            >
              {categoryMeta.label}
            </span>
          )}
        </div>

        <div className="flex gap-2">
          {isConfirmed && (
            <Btn variant="ghost" size="sm" onClick={() => onChange({ ...question, confirmed: false })}>Edit</Btn>
          )}
          {canRemove && (
            <button onClick={onRemove} className="text-text-ter hover:text-error-on p-1 transition-colors">✕</button>
          )}
        </div>
      </div>

      {isConfirmed ? (
        <p className="text-text-pri leading-relaxed">{question.questionText}</p>
      ) : (
        <div className="flex flex-col gap-4">
          <div>
            <Label required>Question text</Label>
            <textarea
              value={question.questionText}
              onChange={(e) => onChange({ ...question, questionText: e.target.value })}
              placeholder="Describe what is being evaluated…"
              rows={2}
              className="w-full px-3 py-2 text-base rounded-md border border-border-sec bg-bg-primary text-text-pri placeholder:text-text-ter outline-none focus:border-lsg-blue focus:ring-3 focus:ring-lsg-blue/10 transition-all resize-none"
            />
          </div>

          <div>
            <Label required>COPC Category</Label>
            <CategorySelector
              value={question.category}
              onChange={(cat) => onChange({ ...question, category: cat })}
            />
          </div>

          {question.subattributes.length > 0 && (
            <div className="flex flex-col gap-2">
              <p className="text-[10px] font-bold text-text-ter uppercase tracking-widest">Subattributes</p>
              {question.subattributes.map((attr) => (
                <AttributeEditor
                  key={attr.id}
                  attribute={attr}
                  isAccountabilityMode={isAccountabilityMode}
                  onChange={(updated) => handleAttributeChange(attr.id, updated)}
                  onRemove={() =>
                    onChange({
                      ...question,
                      subattributes: question.subattributes.filter((a) => a.id !== attr.id),
                    })
                  }
                />
              ))}
            </div>
          )}

          <div className="flex items-center justify-between border-t border-border-ter pt-4">
            <Btn
              variant="ghost"
              size="sm"
              onClick={() => onChange({ ...question, subattributes: [...question.subattributes, EMPTY_ATTRIBUTE()] })}
            >
              <span className="text-lg">+</span> Add subattribute
            </Btn>
            <Btn
              variant="primary"
              onClick={() => onChange({ ...question, confirmed: true })}
              disabled={!question.questionText.trim() || !question.category}
            >
              Confirm question
            </Btn>
          </div>
        </div>
      )}
    </SectionCard>
  );
}