import {basicSetup} from "codemirror"
import {EditorState} from "@codemirror/state"
import {EditorView, keymap} from "@codemirror/view"
import {indentWithTab} from "@codemirror/commands"
import {} from "@codemirror/lang-javascript"
import {inflate, deflate} from "pako"

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

let src;
const base64DeflateUtf8Src = new URLSearchParams(window.location.search).get("s");
if (base64DeflateUtf8Src === null) {
    src = `
a := 1 .. 2 | "l"
b := "He$a"
c := [
  b
  ", W"
  "rld!"
]

c() >> JOIN["o"]
`;
} else {
    const deflateUtf8Src = new Uint8Array([...atob(base64DeflateUtf8Src)].map(c => c.charCodeAt(0)));
    const utf8Src = inflate(deflateUtf8Src);
    src = new TextDecoder().decode(utf8Src);
}
window.input = new EditorView({
    state: EditorState.create({
        doc: src,
        extensions: [customKeymap, basicSetup]
    }),
    parent: document.getElementById("input_container")
});

window.inflate = function(data) {
    return inflate(data);
};

window.deflate = function(data) {
    return deflate(data);
};
