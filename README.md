Image Resizer
=============

This is a small project to resize images that have been uploaded to Canvas to be used as course images. Canvas at the moment doesn't limit the size or resolution of uploaded images and a user having a dashboard with lots of large images on it causes performance problems.

Running
=======

```
Usage: imagecheck [-hV] [--debug] [COMMAND]
Processes course images in canvas
      --debug     Output debugging information
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  help     Displays help information about the specified command
  restore  Uploads course images to courses.
  resize   Scans all course images and attempts to resize those that are too
             large

```

Notes
=====

To get an executable that works on UNIX (Linux/Mac) we just concatenate a shell script onto the front of the JAR and drop result in `./target/imagecheck`. This allows you to run it simply from the command line.

You can adjust the logging level with `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` but to do this you need to run the program without the wrapper so the arguments are passed to the JVM and not interprited as program arguments (which can't be found). This is useful when passing the `--debug` flag which at the moment outputs all the feign calls  Eg:

    java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -jar target/imagecheck --debug resize -d -c test.properties
    
