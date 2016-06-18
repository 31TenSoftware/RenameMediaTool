## RenameMediaTool
a tool for correcting media EXIF data, and last modified and date created, and chronologically renaming.

Check out the [releases](https://github.com/31TenSoftware/RenameMediaTool/releases) if you just want to use it, but a few things you should know:

1. The software runs into 2 steps. The first step it runs through a directory of media and gets information and determines date/times and order. Then it tells you if it found anything it couldn't figure out, and **creates a changes.csv** file in the directory you selected that lists all the changes the program is about to perform.
  - if the `newDateTime` column is populated, it will try to write a new date/time lastModified and dateCreated, and EXIF if it's a jpeg.
  - if there's a value in `newFilename` it's going to rename the file with a better name.
2. There's a `Stagger date/time of files with the same date/time` checkbox -- what this does is order the files by their current filename, then stagger their date/times by 1 second so that their date/times match the filename ordering. This is useful in cases where a bunch of media has been scanned in and just given a date/time of May 5, 1986 at 12pm: you might want date/times staggered to reflect that not all the media happened at the same time.
3. [This](https://github.com/31TenSoftware/RenameMediaTool/blob/master/src/com/thirtyonetensoftware/renamemediatool/ProcessWorker.java#L84-L89) is the current order filenames are evaluated in. The program will try to date parse the filename using the first pattern that matches. i.e. The pattern for `YearMonthDayTime` and `MonthDayYearTime` are the same, so whichever pattern is evaluated second will never be matched.
  - If this is unacceptable, fork the project, change the `FilenameTester`s around and build your own version. run the build.xml somehow to make your own jars; (i'm not much help, i just tell intellij to build it).
