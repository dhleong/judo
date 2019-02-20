Judo Mapping
============

Judo has some built-in auto-mapping support. In theory, we support MSDP
and GMCP-based auto-mapping and manual auto-mapping by intercepting
directional navigation (eg: `w`, `n`, etc.), but it's not super robust.

## Settings

Name              |  Default  |  Description
------------------|-----------|-------------
`map:autorender`  |  `false`  | If `true`, the map is automatically rendered when you move
`map:autoroom`    |  `false`  | If `true`, new rooms are added to the map automatically as you move. You may want to set this to `false` if you enable `map:automagic`
`map:automagic`   |  `false`  | If `true`, we will attempt to use MSDP/GMCP to automagically create maps. This MUST be set BEFORE creating or loading a map

## Map Output

By default will cause Judo to render maps into the primary output
window. However, you can also create a new window with `hsplit()` or
`vsplit()` and render the map into that:

```python
primary = judo.current.window
mapWindow = vsplit(20)

# switch focus back to primary window
judo.current.window = primary

# set the target window for map output
judo.mapper.window = mapWindow
```

If `map:autorender` is `true`, the map should be rendered for you
automatically. If you set `map:autorender` to `false`, however, you
will have to manually render the map by calling:

```python
judo.mapper.render()
```

You might do this from a trigger, as part of an alias, etc.

## Persisting map data

Judo currently supports the tintin++ map format. Here is an example
script demonstrating how to save and load a map:

```python
def mapToFile(mapFile):
    config('map:automagic', True)
    config('map:autorender', True)

    if os.path.exists(mapFile):
        judo.mapper.load(mapFile)
        print "Loaded map %s" % mapFile
    else:
        # create a new map and save it immediately
        # so we can just do judo.mapper.save() later
        judo.mapper.createEmpty()
        judo.mapper.saveAs(mapFile)
        print "Created empty map %s" % mapFile

    primaryWindow = judo.current.window
    if judo.mapper.window.id == primaryWindow.id:
        # split a new window to render the map into
        judo.mapper.window = vsplit(20)
        judo.current.window = primaryWindow

    # register an event to save the map on disconnect
    def persistMap():
        if judo.mapper.current is not None:
            judo.mapper.save()
            print "Saved map"
        else:
            print "No map to save"

    event("DISCONNECTED", persistMap)
```
