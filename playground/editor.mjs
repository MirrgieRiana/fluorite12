import {basicSetup} from "codemirror"
import {EditorState} from "@codemirror/state"
import {EditorView, keymap} from "@codemirror/view"
import {indentWithTab} from "@codemirror/commands"
import {} from "@codemirror/lang-javascript"

const customKeymap = keymap.of([
    {
        key: "F9",
        run: view => {
            window.run()
            return true;
        }
    },
    {
        key: "Ctrl-Enter",
        run: view => {
            window.run()
            return true;
        }
    },
    indentWithTab
]);

window.input = new EditorView({
    state: EditorState.create({
        doc: `
a := 1 .. 2 | "l"
b := "He$a"
c := [b, ", W", "rld!"]

c[] >> JOIN["o"]
`,
        extensions: [customKeymap, basicSetup]
    }),
    parent: document.getElementById("input_container")
});
