/** @module hooks/usePeek */

import { useState } from "react";

/**
 * Press-and-hold reveal behaviour for password/PIN inputs.
 *
 * Returns a boolean indicating whether the field should be visible,
 * and an event handler object to spread onto the reveal button.
 * Releasing the button — or moving the cursor/finger away while held —
 * immediately hides the value again.
 *
 * @returns {[boolean, object]} [peeking, handlers]
 *
 * @example
 * const [peeking, peekHandlers] = usePeek();
 *
 * <input type={peeking ? "text" : "password"} />
 * <button type="button" {...peekHandlers}>👁</button>
 */
export function usePeek() {
  const [peeking, setPeeking] = useState(false);

  const handlers = {
    onMouseDown:  () => setPeeking(true),
    onMouseUp:    () => setPeeking(false),
    onMouseLeave: () => setPeeking(false),
    onTouchStart: (e) => { e.preventDefault(); setPeeking(true);  },
    onTouchEnd:   () => setPeeking(false),
  };

  return [peeking, handlers];
}