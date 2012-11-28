# jmx-repl

A JMX client with a file system style. You use `ls` to check all the mbeans, use `cd` to go down one level, and use `cat` to check the attribute value of a mbean's attribute.

In `jmx-repl` the following jmx bean:

```clojure
java.lang:type=MemoryPool,name=CMS Old Gen
```

translates to the following path:

```clojure
/java.lang/MemoryPool/CMS Old Gen
```
i.e. in `jmx-repl`, `java.lang`, `MemoryPool`, `CMS Old Gen` are all directories, the attributes of this bean are "file" in `jmx-repl`.

## Usage

```clojure
xumingmingv:jmx-repl(git:master)$ lein repl
REPL started; server listening on localhost port 44034
	 help -- print this help
	 ls   -- list the items in current directory
	 cd   -- enter a folder
	 cat  -- print the value of an item
	 pwd  -- show the current path
	 exit -- exit
[/] => ls
	JMImplementation
	com.sun.management
	java.lang
	java.util.logging
[/] => cd java.lang
[/java.lang] => ls
	ClassLoading
	Compilation
	GarbageCollector
	Memory
	MemoryManager
	MemoryPool
	OperatingSystem
	Runtime
	Threading
[/java.lang] => cd MemoryPool
[/java.lang/MemoryPool] => ls
	CMS Old Gen
	CMS Perm Gen
	Code Cache
	Par Eden Space
	Par Survivor Space
[/java.lang/MemoryPool] => cd CMS Old Gen
[/java.lang/MemoryPool/CMS Old Gen] => ls
	CollectionUsage
	CollectionUsageThreshold
	CollectionUsageThresholdCount
	CollectionUsageThresholdCount
	CollectionUsageThresholdExceeded
	CollectionUsageThresholdSupported
	MemoryManagerNames
	Name
	PeakUsage
	Type
	Usage
	UsageThreshold
	UsageThresholdCount
	UsageThresholdExceeded
	UsageThresholdSupported
	Valid
[/java.lang/MemoryPool/CMS Old Gen] => cat CollectionUsage
{:committed 0, :init 65404928, :max 110362624, :used 0}
[/java.lang/MemoryPool/CMS Old Gen] => exit
xumingmingv:jmx-repl(git:master)$
```

## License

Copyright (C) 2012 xumingming

Distributed under the Eclipse Public License, the same as Clojure.
