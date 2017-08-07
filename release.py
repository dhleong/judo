#!/usr/bin/env python
#
# Release script for Judo
#

from collections import OrderedDict

try:
    from hostage import *
except ImportError:
    print "!! Release library unavailable."
    print "!! Use `pip install hostage` to fix."
    print "!! You will also need an API token in .github.token,"
    print "!!  a .hubrrc config, or `brew install hub` configured."
    print "!! A $GITHUB_TOKEN env variable will also work."
    exit(1)

#
# Globals
#

notes = File(".last-release-notes")
latestTag = git.Tag.latest()

def formatIssue(issue):
    return "- {title} (#{number})\n".format(
            number=issue.number,
            title=issue.title)

def buildLabeled(labelsToTitles):
    """Given a set of (label, title) tuples, produces an
    OrderedDict whose keys are `label`, and whose values are
    dictionaries containing 'title' -> `title`, and
    'content' -> string. The iteration order of the dictionary
    will preserve the ordering of the provided tuples
    """
    result = OrderedDict()
    for k, v in labelsToTitles:
        result[k] = {'title': v, 'content': ''}
    return result

def buildDefaultNotes(_):
    if latestTag is None:
        return ''

    logParams = {
            'path': latestTag.name + "..HEAD",
            'grep': ["Fix #", "Fixes #", "Closes #"],
            'pretty': "format:- %s"}
    logParams["invertGrep"] = True
    msgs = git.Log(**logParams).output()

    contents = ''

    lastReleaseDate = latestTag.get_created_date()
    if lastReleaseDate.tzinfo:
        # pygithub doesn't respect tzinfo, so we have to do it ourselves
        lastReleaseDate -= lastReleaseDate.tzinfo.utcoffset(lastReleaseDate)
        lastReleaseDate.replace(tzinfo=None)

    closedIssues = github.find_issues(state='closed', since=lastReleaseDate)

    labeled = buildLabeled([
        ['feature', "New Features"],
        ['enhancement', "Enhancements"],
        ['bug', "Bug Fixes"],
        ['_default', "Other resolved tickets"],
    ])

    if closedIssues:
        for issue in closedIssues:
            found = False
            for label in labeled.iterkeys():
                if label in issue.labels:
                    labeled[label]['content'] += formatIssue(issue)
                    found = True
                    break
            if not found:
                labeled['_default']['content'] += formatIssue(issue)

    for labeledIssueInfo in labeled.itervalues():
        if labeledIssueInfo['content']:
            contents += "\n**{title}**:\n{content}".format(**labeledIssueInfo)

    if msgs: contents += "\n**Notes**:\n" + msgs
    return contents.strip()

#
# Verify
#

version = verify(File("build.gradle")
        .filtersTo(RegexFilter("version: '(.*)'"))
        ).valueElse(echoAndDie("No version!?"))
versionTag = git.Tag(version)

verify(versionTag.exists())\
    .then(echoAndDie("Version `%s` already exists!" % version))

#
# Make sure all the tests pass
#

gradlew = gradle.Gradle()
verify(gradlew).executes('test').orElse(die())

#
# Build the release notes
#

contents = verify(notes.contents()).valueElse(buildDefaultNotes)
notes.delete()

verify(Edit(notes, withContent=contents).didCreate())\
        .orElse(echoAndDie("Aborted due to empty message"))

releaseNotes = notes.contents()

#
# Do the actual build
#

verify(gradlew).executes('jar').orElse(die())

jarFile = File('judo/build/libs/judo-%s.jar' % version)
verify(jarFile).exists().orElse(echoAndDie("Failed to build %s" % jarFile.path))

#
# Upload to github
#

print "Uploading to Github..."

verify(versionTag).create()
verify(versionTag).push("origin")

gitRelease = github.Release(version)
verify(gitRelease).create(body=releaseNotes)

print "Uploading", jarFile.path
verify(gitRelease).uploadFile(jarFile.path, 'application/octet-stream')

#
# Success! Now, just cleanup and we're done!
#

notes.delete()

print("Done! Published %s" % version)

# flake8: noqa
