#!/usr/bin/env python3
#
# Release script for Judo
#

import re
import hashlib
from collections import OrderedDict

# pylint: disable=unused-wildcard-import,wildcard-import
try:
    from hostage import *
except ImportError:
    print("!! Release library unavailable.")
    print("!! Use `pip install hostage` to fix.")
    print("!! You will also need an API token in .github.token,")
    print("!!  a .hubrrc config, or `brew install hub` configured.")
    print("!! A $GITHUB_TOKEN env variable will also work.")
    exit(1)

#
# Globals
#

notes = File(".last-release-notes")
latestTag = git.Tag.latest(branch = "main")

def sha256(filePath, blockSize=65536):
    # borrowed from: https://gist.github.com/rji/b38c7238128edf53a181
    sha = hashlib.sha256()
    with open(filePath, 'rb') as f:
        for block in iter(lambda: f.read(blockSize), b''):
            sha.update(block)
    return sha.hexdigest()

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

    notesContents = ''

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
            for label in labeled.keys():
                if label in issue.labels:
                    labeled[label]['content'] += formatIssue(issue)
                    found = True
                    break
            if not found:
                labeled['_default']['content'] += formatIssue(issue)

    for labeledIssueInfo in labeled.values():
        if labeledIssueInfo['content']:
            notesContents += "\n**{title}**:\n{content}".format(**labeledIssueInfo)

    if msgs: notesContents += "\n**Notes**:\n" + msgs
    return notesContents.strip()

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

print("Uploading to Github...")

verify(versionTag).create()
verify(versionTag).push("origin")

gitRelease = github.Release(version)
verify(gitRelease).create(body=releaseNotes)

print("Uploading", jarFile.path)
verify(gitRelease).uploadFile(jarFile.path, 'application/octet-stream')

#
# Update homebrew repo
#

print("Updating homebrew...")

jarUrl = 'https://github.com/dhleong/judo/releases/download/%s/judo-%s.jar' % (version, version)
jarSha = sha256(jarFile.path)

homebrewConfig = github.Config("dhleong/homebrew-judo")
formulaFile = github.RepoFile("/Formula/judo.rb", config=homebrewConfig)
oldContents = str(formulaFile.read())

newContents = oldContents
newContents = re.sub('url "[^"]+"', 'url "%s"' % jarUrl, newContents)
newContents = re.sub('sha256 "[^"]+"', 'sha256 "%s"' % jarSha, newContents)

print("     url <-", jarUrl)
print("  sha256 <-", jarSha)
commit = 'Update for v%s' % version
verify(formulaFile).write(newContents, commitMessage=commit)

#
# Success! Now, just cleanup and we're done!
#

notes.delete()

print("Done! Published %s" % version)

# flake8: noqa
