judo [![Build Status](http://img.shields.io/travis/dhleong/judo.svg?style=flat)](https://travis-ci.org/dhleong/judo)
====

*A more elegant way to play in the [MUD][1]*

## What

Judo is a terminal-based, modal MUD client inspired by [Vim][2].  If you're unfamiliar
with Vim and do a lot of typing or text editing, go take a look! On a Unix system you
can probably run `vimtutor` from your favorite terminal to get an intro.

Being modal means that the keys you type don't always just go into the "input buffer"
for sending to the MUD server. Instead, each key press, or sequence of key presses,
can be bound to do many different things, which very based on the current "mode."

### Normal Mode

Normal Mode is largely identical to Vim's, where keys are used to move around the input
buffer and perform large editing *verbs*, or *actions*. A *verb* takes an *object*
(more commonly referred to as *motions*), just like in most languages. For example,
if you use the `delete` verb (bound to the `d` key) with the `word` object (bound to
the `w` key), you will *delete* until the start of the next "word." This is done by
simply typing `dw` in Normal Mode. There's also an `end of line` object, `$`, so
typing `d$` will delete from the current position all the way to the end of the line.

There are [many][3] [resources][4] for [learning][5] [about][6] [vim][7], but
hopefully this very brief intro will convince you to give it a shot.

Note that not all verbs and motions from Vim are implemented in Judo—yet!

### Insert Mode

Insert Mode is also largely identical to Vim's, and is equivalent in function to the
*only* mode in most other editors. When you type in Insert Mode, the text gets
*inserted* into the input buffer. Pressing the escape key, or hitting `ctrl+[`,
in Insert Mode—or any other mode—will send you back to Normal Mode.

### Command Mode

Command Mode is similar to Vim's, but instead of executing vimscript, it executes
Python code. With functions like `disconnect()` which don't take any arguments, you
can omit the parentheses.

### Custom Modes

One interesting feature of Judo is the ability to create custom modes. We'll go
into more depth on the mechanics of this feature in the [scripting documentation][10],
but the idea is to be able to have a blank slate on which to create whatever
mappings you like. For myself, I created a `nav` mode, in which the `hjkl`, and `uon,`
keys map to the directions: `n`, `e`, `s`, `w`, and `nw`, `ne`, `sw`, `se`,
respectively. Holding shift while pressing these keys `open` the corresponding
direction.


## How

Judo is implemented in [Kotlin][8] because I wanted to learn more about it after Google
announced their official support for it on Android. I'd been tinkering with it a bit,
but it was time to really get my hands dirty. Also, the JVM is pretty ubiquitous,
and I didn't want to have to deal with compiling stuff (although it does mean Judo
uses a good deal more memory than Vim which inspired it).

### Installing on macOS

You can easily install a build on macOS using [Homebrew][12]:

    brew tap dhleong/judo
    brew install judo

Then, simply use the installed `judo` binary as described below.

### Installing on others

Builds can be found on the [releases page][13], or you can build your own
using the included Gradle script:

    ./gradlew jar

Then, to run:

    java -jar judo/build/libs/judo-{version}.jar

Since Judo needs tight control over the terminal, you unfortunately cannot simply
do `./gradle run`, which also means running a debug build from IntelliJ won't work
as expected. Instead, you can run like this:

    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar judo/build/libs/judo-{version}.jar

and connect IntelliJ's debugger to it by creating a "Remote" debug run configuration.

However you invoke the jar, you can save that as a script and use it in place of `judo`
in the examples below.

Note that you may also need to install [Java 8][14] if it isn't already available.

### Connecting to a Server

You have a couple options here:

1. Pass host and port to the CLI:

        judo myawesomeserver.com 5656

2. Use the `:connect(host, port)` command while `judo` is running

3. Create a world script containing `connect(host, port)` and pass *that* to the CLI:

        judo myawesomescript.py

### Configuring

Judo tries to load `$HOME/.config/judo/init.py` or `$HOME/.config/judo/init.js`
every time it loads—use whichever language you prefer—so that is a great place
to put common mappings, etc.

### Scripting

Judo uses [Python][9] or [Javascript][15] for configuration and scripting. See
[Scripting.md][11] for more details, or run `help` in Command Mode to get
started.


## Why

I've played MUDs off and on throughout my life, but when I started getting into
them again recently, I found that trying to navigate around worlds quickly put
my hand into a very RSI-inducing position (I don't have a keyboard with a
numpad). I was inspired by my favorite editor to create something modal, so I
could bind the hjkl keys to the cardinal directions and keep my hands
comfortably on the home row.


[1]: https://en.wikipedia.org/wiki/MUD
[2]: http://www.vim.org/
[3]: http://yannesposito.com/Scratch/en/blog/Learn-Vim-Progressively/
[4]: https://stackoverflow.com/a/1220118
[5]: http://derekwyatt.org/vim/tutorials/
[6]: https://medium.com/usevim
[7]: http://vimcasts.org/
[8]: https://kotlinlang.org/
[9]: https://www.python.org/
[10]: ./docs/Scripting.md#custom-modes
[11]: ./docs/Scripting.md
[12]: https://brew.sh/
[13]: https://github.com/dhleong/judo/releases
[14]: http://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html
[15]: https://javascript.info/
