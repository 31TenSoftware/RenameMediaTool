## RenameMediaTool
### This repository has been archived in favor of MediaUtilityBelt - an updated version of this application
a tool for correcting media EXIF data, last modified, date created, and chronologically renaming.

Check out the [releases](https://github.com/31TenSoftware/RenameMediaTool/releases) if you just want to use it, but a few things you should know:

- `RenameMediaTool` works in 2 steps: 
  1. It goes through a directory and finds media files. It gets information on those files and determines date/times and order. It tells you if it found anything it couldn't figure out. Review the output for correctness:
      - if the `newDateTime` column is populated, it will try to write a new date/time for lastModified, dateCreated, and EXIF if it's a jpeg.
      - if there's a value in `newFilename` it's going to rename the file with a better name.
  2. Then you can click `Write Changes` to make those changes.
- There's a `Stagger date/time of files with the same date/time` checkbox -- this orders files by their current filename, then staggers their date/times by 1 second so that their date/times match the filename ordering.
  - This is useful in cases where a bunch of media has been scanned in and just given a date/time of May 5, 1986 at 12pm: you might want date/times staggered to reflect that not all the media happened at the same time.
- [This](https://github.com/31TenSoftware/RenameMediaTool/blob/master/src/com/thirtyonetensoftware/renamemediatool/ProcessWorker.java#L84-L89) is the current order filenames are evaluated in. The program will try to date parse the filename using the first pattern that matches. i.e. the pattern for `YearMonthDayTime` and `MonthDayYearTime` are the same, so whichever pattern is evaluated second will never be matched.
    - If this is unacceptable, fork the project, change the `FilenameTester`s around and build your own version. run the build.xml somehow to make your own jars; (i'm not much help, i just tell IntelliJ to build it).
