Judo Vim Compatibility
======================

While we strive to feature as many Vim features as we can, as *accurately*
as we can, some inevitably don't make sense, aren't necessary, or just
haven't been thought of yet.

## Supported Features

Here's a list of features we definitely support:

* Motions
    * `b B w W e E ge gE f F h l t T 0 $ ; ,`
    * `iw iW aw aW i" i' i( i) ib a" a' a( a) ab`
* Actions (all with counts)
    * `a A D C i I p P r x X ~`
    * With motions: `d c gu gU g~`
* Scrolling
    * `<ctrl f>  <ctrl b>`
* Searching with `/` and `n`, `N`
    * This searches through the output window
* Registers, including `"*` and `"+` for the clipboard
    * Note that both are identical on all platforms; we do not support X11 selections
* Key Mappings (See [Scripting.md](Scripting.md))
* Split Windows

## Differences from Vim

Since Vim is primarily a text editor, and Judo is a MUD client (with
excellent text editing support), some things will not translate well.

### `j/k` scroll input history, not output lines

Judo editing is always single-line, much like the Vim bindings for bash
shells, or similar. So, `j` and `k` scroll the input history rather than
being vertical motions.

### Mappings with modifier keys

Judo supports these, but currently requires you use the format:

    <ctrl w>

instead of

    <ctrl-w>

or

    <c-w>

This may change in the future.

### `<ctrl r>` is for reverse input history search

This one is taken from bash vim bindings: since `/` is used for searching
output, we use `<ctrl r>` to search through previous input. In this mode,
the text you type will be used to search backwards through history for
a matching line. You can provide multi-word filtering by separating parts
by a space. For example, if you know you sent, for example,
`tell kaylee I've got your part` and want to send it again, you could
search for:

    te ka

and Judo will probably find it, matching `te` to "tell" and `ka` to "kaylee."

If you've told Kaylee a bunch of things in the meantime, however, you might
try:

    te k p

or

    t pa

or any variation thereof to try to match "part."

Pressing `<ctrl r>` again while in this mode will continue searching back
through the history with the same text.

Note that we don't yet support undo/redo, but if we do, redo will probably
be put on `U` instead of `<ctrl r>`. Luckily, you can remap this if it's
not to your liking!
