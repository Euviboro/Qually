import { Btn } from "../../components/ui/Btn";
import { Label } from "../../components/ui/Label";
import { SectionCard } from "../../components/ui/SectionCard";
import { CategorySelector, COPC_CATEGORIES } from "./CategorySelector";
import { AttributeEditor } from "./AttributeEditor";
import { EMPTY_ATTRIBUTE } from "../../hooks/useNewProtocol";

export function QuestionCard({ question, index, onChange, onRemove, canRemove }) {
  const isConfirmed  = question.confirmed;
  const categoryMeta = COPC_CATEGORIES.find((c) => c.value === question.category);

  return (
    <SectionCard className={`transition-all duration-300 ${isConfirmed ? "border-gray-200" : "border-blue-400 ring-4 ring-blue-500/5"}`}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2.5">
          <span className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold font-mono
            ${isConfirmed ? "bg-gray-100 text-gray-400" : "bg-blue-100 text-blue-600"}`}>
            {index + 1}
          </span>
          <span className={`text-sm font-bold uppercase tracking-tight ${isConfirmed ? "text-gray-500" : "text-blue-600"}`}>
            {isConfirmed ? "Question confirmed" : "New question"}
          </span>
          {isConfirmed && categoryMeta && (
            <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${categoryMeta.bg} ${categoryMeta.color}`}>
              {categoryMeta.label}
            </span>
          )}
        </div>

        <div className="flex gap-2">
          {isConfirmed && (
            <Btn variant="ghost" size="sm" onClick={() => onChange({ ...question, confirmed: false })}>Edit</Btn>
          )}
          {canRemove && (
            <button onClick={onRemove} className="text-gray-300 hover:text-red-500 p-1 transition-colors">✕</button>
          )}
        </div>
      </div>

      {isConfirmed ? (
        <p className="text-gray-900 leading-relaxed">{question.text}</p>
      ) : (
        <div className="flex flex-col gap-4">
          <div>
            <Label required>Question text</Label>
            <textarea
              value={question.text}
              onChange={(e) => onChange({ ...question, text: e.target.value })}
              placeholder="Describe what is being evaluated…"
              rows={2}
              className="w-full px-3 py-2 text-base rounded-md border border-gray-200 bg-white outline-none focus:border-blue-500 focus:ring-3 focus:ring-blue-500/10 transition-all resize-none"
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
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Subattributes</p>
              {question.subattributes.map((attr) => (
                <AttributeEditor
                  key={attr.id}
                  attribute={attr}
                  onChange={(updated) =>
                    onChange({
                      ...question,
                      subattributes: question.subattributes.map((a) => (a.id === attr.id ? updated : a)),
                    })
                  }
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

          <div className="flex items-center justify-between border-t border-gray-50 pt-4">
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
              disabled={!question.text.trim() || !question.category}
            >
              Confirm question
            </Btn>
          </div>
        </div>
      )}
    </SectionCard>
  );
}