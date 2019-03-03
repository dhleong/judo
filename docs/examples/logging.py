import os
from datetime import date

# fake imports to satsify flake8:
# pylint: disable=unused-import
try:
    from judo import logToFile
except ImportError, e:
    pass

def pickLogFile(dirName):
    """Pick the full path to a log file for the current day

    :dirName: The subdirectory of ~/judo for this world
    :returns: The full path to the log file

    """

    # name the log file based on the current date
    dateStr = date.today().isoformat()

    # this assumes you store your logs in in a folder called "logs"
    # inside the folder where your script file is. For example:
    #   ~/judo/awesome-mud/logs/2019-01-21.html
    home = os.path.expanduser('~')
    path = '%s/judo/%s/logs/%s.html' % (home, dirName, dateStr)

    # if the logs directory doesn't exist... make it!
    dirPath = os.path.dirname(path)
    if not os.path.isdir(dirPath):
        os.makedirs(dirPath)

    # return the full path
    return path

def logToPickedFile(dirName):
    """Start logging in html format to an appropriate log file

    :dirName: The subdirectory of ~/judo for this world

    """

    # pick the file path (see above)
    logFile = pickLogFile(dirName)

    # see :help logToFile in Judo. Basically, `html` here means to log
    # in HTML format. Also supported are `raw` which has the raw ANSI
    # codes the server sent, and `plain` which is just the text---no
    # decoration.
    # Having `append` at the beginning means if you call `logToFile`
    # again with the same log file path, new stuff will be appended
    # to the original log file, instead of replacing the old one.
    options = 'append html'

    # start logging!
    logToFile(logFile, options)


##
## NOTE: You can copy and paste all the above into your `init.py`, and
## use it in your world script with something like this:
##

# fake imports to satsify flake8:
# pylint: disable=unused-import
try:
    from judo import event
    from init import logToPickedFile
except ImportError, e:
    pass

# as soon as we're connected...
@event("CONNECTED")
def on_connect():
    # ... start logging immediately
    logToPickedFile('awesome-mud')

    # NOTE: in this example, this file is assumed to be located at
    # `~/judo/awesome-mud/world.py`
