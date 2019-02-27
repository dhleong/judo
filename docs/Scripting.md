Judo Scripting
==============

Scripting is integral to Judo, as any sort of persistent configuration is done
in a scripting language. Currently, Judo supports [Python][1] and
[Javascript][5], but all examples below will be in Python because it has some
convenient syntax that the Javascript support does not.

## Connecting to a World

The two most basic functions you'll want to know when writing a world script
are `isConnected()` and `connect()`. For example:

```python
if isConnected():
    # connect to a server on port 1234
    connect('serenity.space', 1234)
```

By wrapping the `connect()` in `if isConnected()`, you can use `:reload`
re-read your world settings and mappings without having to disconnect.

## Basic Config

Basic settings are modified by using the `config()` function. For example,
to copy and paste to and from the clipboard, use:

```python
config('clipboard', 'unnamed')
```

A current list of supported settings and their current values can be found
with `:config`.

### Aliases, Triggers, and Prompts—oh my!

These three common MUD client features all function similarly in Judo,
and share features. In a basic sense, they all take text from somewhere,
extract pieces of that text, do something to those pieces, and send the
result somewhere else:

- **Aliases**: take text from user input and send it to the game server
- **Triggers**: take text from the game server and send it to script functions
- **Prompts**: take text from the game server and send it to the status line

These three features are available via the `alias()`, `trigger()`,
and `prompt()` functions, respectively. All three can be used either as
functions or as decorators, eg:

```python
# Note the ^ here; it means only do this if it matches the beginning
# of the input. The $1 is a variable placeholder, so this would match
# "a button," for example, and send "activate button"
alias('^a $1', 'activate $1')

# this is functionally identical to above
@alias('^a $1')
def on_activate(thing):
    # if you return text, that is what gets sent
    return 'activate %s' % thing

@alias('^a $1')
def on_activate(thing):
    # if you don't return anything, you're responsible
    # for `send()`ing things
    send('activate %s' % thing)
```

Note that while `alias()` and `prompt()` can accept text as the second
argument and Judo will automatically substitute in any variables and
pass it on, `trigger()` MUST provide a function as the second argument,
or be used as a decorator.

#### Regular Expressions

Aliases, Triggers, and Prompts all accept a regular expression instead
of text, for fancier matching. For example:

```python
import re
@trigger(re.compile(r'^(\(IMM\).*)$'), 'color')
@trigger(re.compile(r'^(\(OOC\).*)$'), 'color')
@trigger(re.compile(r'^(CommNet.*)$'), 'color')
def on_chat(line):
    # forward to split window
```

This will capture various chat messages and pass them to the `on_chat()`
function. Note the extra flag `'color'`. Normally, ANSI color codes are
stripped from matched text, but if you want to keep them, this flag will
let you do that.

Matched groups from the regular expression will be passed to your
function; in the examples above, there is a single group that matches
the whole line.

Note also how we had multiple `@trigger()` decorators for a single function.
This is a convenient way to handle multiple types of input in the same way.

### Mappings

Judo supports keymappings much like Vim, functions like `nmap`, `nnoremap`,
and so on. For example, to send "save" to a world when you type `gs` from
normal mode, use:

```python
nnoremap('gs', 'isave<cr>')
```

Note that, like vim, mappings literally perform the keystrokes you provide,
so the above would actually enter insert mode (`i`), type "save," then press
the enter key to send.

#### Function mappings

Map commands can also take a function to be called when the mapping is
executed. For example, the above could be rewritten as:

```python
def doSave():
    send('save')

nnoremap('gs', doSave)
```

#### Differences from Vim

Since Judo's first priority is to be a MU\* client, and *not* to edit text,
there are naturally some differences with Vim. For a full list,
see [Vim Compat][4].


## Events

Judo provides a number of events which you can hook into in your scripts
using the `event()` function. `event()` is also a decorator, and we
recommend using it that way for simplicity. For example, to automatically
log in when you connect to a world, you might do this:

```python
@event("CONNECTED")
def on_connect():
    send('mreynolds')
    send('serenity1')
```

### Out-of-band Communication

Judo supports both [MSDP][2] and [GMCP][3] via an events. When Judo
determines that MSDP and/or GMCP are supported, it first emits
`MSDP ENABLED` and `GMCP ENABLED`, respectively. When Judo receives
an event, it broadcasts both a generic `MSDP` or `GMCP` event which
receives the event type and the event argument (if any), and a
type-specific event. For example:

```python
# this...
@event("GMCP")
def on_room(eventType, arg):
    if eventType == 'room.info':
        doSomethingWithRoom(arg)

# is roughly equivalent to this:
@event("GMCP:room.info")
def on_room(room):
    doSomethingWithRoom(room)
```

## Windows

You can create split windows the `hsplit()` function, which will
create a new window by drawing a horizontal line through the current
window and splitting it into a "top" and a "bottom." The new window
will currently always be the one on top. `hsplit()` takes either a
`float` such as `.3` to create a window whose height is a percentage of
the current window, or an `int` such as `10` to explicitly choose a height
in lines.

`hsplit()` and `vsplit()` return a `Window` object, which has the following
attributes and methods:

Name      | Returned Type | Description
----------|---------------|------------
id        | `Int`         | The window's unique, numeric ID
buffer    | `Buffer`      | The window's underlying `Buffer` object
height    | `Int`         | The height of the window in rows
width     | `Int`         | The width of the window in columns
onSubmit  | `(String) -> Unit` | A function to call when input is submitted while this window is focused
`close()` | `None`        | Close the window
`resize(width: Int, height: Int)` | `None` | Resize the window

The `Buffer`, much like in Vim, is what contains the text that `Window`
displays. A `Buffer` object has the following attributes and methods:

Name                   | Returned Type | Description
-----------------------|---------------|------------
id                     | `Int`         | The buffer's unique, numeric ID
`append(line: String)` | `None`        | Append a line of text to the buffer
`clear()`              | `None`        | Clear all text from the buffer
`set(lines: String[])` | `None`        | Replace the entire contents of the buffer

`Buffer` also supports the `len()` function, which will return how many
lines of text it contains.

## Window focus APIs

You can get and set the current window with an API similar to what Vim offers to Python:

```python
# retrieve the currently focused window
old = judo.current.window

# set the currently focused window
judo.current.window = w
```

You can also get the current `tabpage` and `buffer`. The Tabpage object is not incredibly
useful at the moment, but does offer the following attributes:

Name      | Returned Type | Description
----------|---------------|------------
id        | `Int`         | The window's unique, numeric ID
height    | `Int`         | The height of the window in rows
width     | `Int`         | The width of the window in columns

## Custom Modes

Judo supports creating custom Modes, where the only pre-determined mapping
is `<esc>` to get back to Normal Mode. You can create a simple "navigation
mode" like so:

```python
createUserMode('nav')
nnoremap('q', lambda: enterMode('nav'))  # use q to enter nav mode

# all the direction mappings
dirs = {'j': 's',
        'k': 'n',
        'h': 'w',
        'l': 'e',
        'u': 'nw',
        'o': 'ne',
        'n': 'sw',
        ',': 'se',
        's': 'down',
        'w': 'up'}

for key, dir in dirs.iteritems():
    # create a function to send(); we could
    # create a regular map that goes into insert mode,
    # types the direction, hits enter, then returns to
    # nav mode, but this is simpler:
    def sendDir(d=dir): send(d)

    # create the map
    createMap('nav', key, sendDir)

    # for fun, create an extra map so that, if we hold "shift"
    # when pressing the direction, we try to open a door there.
    if key == ',':
        createMap('nav', '<', lambda d=dir: send('open ' + d))
    else:
        createMap('nav', key.upper(), lambda d=dir: send('open ' + d))
```

## Etc.

As you might have noticed, a handful of features are exposed via a global
object called `judo`. Here's a list of all the properties on this object:

Name                        | Returned Type | Description
----------------------------|---------------|------------
`current.buffer`            | Buffer        | The buffer of the currently-focused window.
`current.window`            | Window        | The currently-focused window. Settable.
`current.tabpage`           | Tabpage       | The currently-focused tabpage.
`mapper`                    | Mapper        | See [Mapping.md](Mapping.md) for more info
`scrollLines(count: Int)`   | None          | Scroll the currently-focused window by `count` lines. A positive number goes *back in time*; a negative number goes forward.
`scrollPages(count: Int)`   | None          | Scroll the currently-focused window by `count` pages.
`scrollBySetting(count: Int)`| None         | Scroll the currently-focused window as if by `<CTRL-U>` (positive numbers) or `<CTRL-D>` (negative numbers). See [VimCompat](VimCompat.md#scroll-setting-is-fixed) for more information.
`scrollToBottom()`          | None          | Scroll the currently-focused window to the newest line.



[1]: https://www.python.org
[2]: http://tintin.sourceforge.net/msdp
[3]: https://www.gammon.com.au/gmcp
[4]: ./VimCompat.md
[5]: https://javascript.info/
