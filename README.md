Image Resizer
=============

This is a small project to resize images that have been uploaded to Canvas to be used as course images. Canvas at the moment doesn't limit the size or resolution of uploaded images and a user having a dashboard with lots of large images on it causes performance problems.

Running
=======

```
Usage: imagecheck [-d] [-a=<accountId>] -c=<configFile> [-h=<desiredHeight>]
                  [-o=<originalDirectory>] [-r=<resizedDirectory>]
                  [-w=<desiredWidth>]
Scans all course images and attempts to resize those that are too large
  -a, --account=<accountId>
                  Canvas account to process. (default: 1)
  -c, --config=<configFile>
                  Filename to load config from.
  -d, --dry-run   If set then don't upload/update course image. (default: false)
  -h, --height=<desiredHeight>
                  Desired height of images. (default: 146)
  -o, --originals=<originalDirectory>
                  Directory to put original images in.
  -r, --resized=<resizedDirectory>
                  Directory to put resized images in.
  -w, --width=<desiredWidth>
                  Desired width of images. (default: 262)
```

Notes
=====

To get an executable that works on UNIX (Linux/Mac) we just concatenate a shell script onto the front of the JAR and drop result in `./target/imagecheck`. This allows you to run it simply from the command line.

You can adjust the logging level with `-Dorg.slf4j.simpleLogger.defaultLogLevel=info`
