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
    * `a A D C i I p P r x X y Y ~`
    * With motions: `d c gu gU g~`
* Undo history:
    * `u <ctrl-r> .`
* Scrolling
    * `<ctrl-f>  <ctrl-b>`, `<ctrl-e>  <ctrl-y>`, `<ctrl-d>  <ctrl-u>` (see
      [Differences](#scroll-setting-is-fixed) below)
* Searching with `/` and `n`, `N`
    * This searches through the output window
* Registers, including `"*` and `"+` for the clipboard
    * Note that both are identical on all platforms; we do not support X11 selections
* Key Mappings (See [Scripting/Mappings](Scripting.md#mappings))
* Split Windows (See [Scripting/Windows](Scripting.md#windows))
    * Navigation can be performed with the `<ctrl-w>k` / `<ctrl-w>j` mappings
      as expected; when focused on a split window, scrolling and searching work
      on that window instead of the main output window (as you might expect).
* Command Line mode
    * Press `<ctrl-f>` in Command Mode or when in a prompt created by `input()`
      to edit your input in Normal Mode. Press enter to submit as normal, or
      `<ctrl-c>` to cancel (`input()` will return `null` in this case).

## Differences from Vim

Since Vim is primarily a text editor, and Judo is a MUD client (with
excellent text editing support), some things will not translate well.

### `j/k` scroll input history, not output lines

Judo editing is always single-line, much like the Vim bindings for bash
shells, or similar. So, `j` and `k` scroll the input history rather than
being vertical motions.

### `<ctrl-s>` is for reverse input history search

This one is borrowed from bash, where it was originally `<ctrl-r>`:
since `/` is used for searching output, we use `<ctrl-s>` to search
through previous input. In this mode, the text you type will be used to
search backwards through history for a matching line. As an added bonus,
you can provide multi-word filtering by separating parts by a space.
For example, if you know you previously  sent, for example,
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

Pressing `<ctrl-s>` again while in this mode will continue searching back
through the history with the same text.

If you prefer the old `<ctrl-r>` mapping, feel free to restore it yourself:

```python
nnoremap('<ctrl-r>', '<ctrl-s>')
```

But don't forget to create a new mapping for "redo"!

### Command mode is a scripting REPL

While Vim has special support for things called "Commands" that you can use
from Command mode (like `:q`), Command mode in Judo is mostly just input for
the scripting runtime. Some special syntax is available for convenience—for
example you can use `:q` like normal, `:disconnect` instead of
`:disconnect()`—but in general you still have to use the correct syntax for
the scripting language.

This could change in the future, but for now its simpler to just use language
syntax, since the scripting language would have no convenient way to make use
of commands the way Vimscript does.


### Only one scripting language at a time

Vim has an embedded language, Vimscript, which supports embedding and calling
other scripting languages. While Judo supports multiple scripting languages
(currently Python and Javascript) you cannot mix and match. Judo picks which
language to use on startup based on:

1. If you provide a script file path when starting, the appropriate language
   for that script is used.
2. If you have an `init.py` script in `~/.config/judo`, Python is used.
3. If you have an `init.js` script in `~/.config/judo`, Javascript is ussed.


### `echo()` does not have a pager (yet)

In Vim if you use `echo` to display more content than fits on a screen, Vim
will let you page through it. Judo doesn't support that yet, so you will only
see the most-recently-echo'd content if it surpasses the displayable area.

### `scroll` setting is fixed

In Vim, the value of `scroll` is changed whenever the Window is resized;
furthermore, it is local to each Window. In Judo, currently this value is
global, and is not changed unless you change it. This also means that any count
provided to `<CTRL-U>` or `<CTRL-D>` is ignored. If the value is `0` or
negative, half of the window's height is used as the scroll amount; otherwise,
the value of the `scroll` setting is used. Part of the reason for this is that
Judo does not yet have Window-local (or Buffer-local) settings.
