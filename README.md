# clj4azure-storage

`clj4azure-storage` is a Clojure wrapper for the Azure Storage SDK.

`clj4azure-storage` repo available at: [https://github.com/BrunoBonacci/clj4azure-storage](https://github.com/BrunoBonacci/clj4azure-storage)

Authors:

  - Bruno Bonacci
  - Dayo Oliyide


## Usage

This is still a work in progress, currently we support the following services/features

  * Azure Storage
    * Blobs
      * Create container
      * list blobs in a container
      * download blobs from a container
      * upload blobs(files) into containers

To use the library, add the following dependency to your `project.clj`

    [clj4azure/clj4azure-storage "0.1.0"]

and then in your namespace add:

```Clojure

(ns myproject
    (:refer-clojure :exclude [list])
    (:require [clj4azure.storage.blob :refer :all]))
         
```

### Storage / blobs


Connect to a container (either existing or create one if needed):

```Clojure

(def cnt (blob-container 
          {:storage-account-name "my-account"
           :storage-account-key  "my-account-key"
           :storage-container    "my-container"}))
           
(def new-cnt (blob-container 
              {:storage-account-name "my-account"
               :storage-account-key  "my-account-key"
               :storage-container    "new-container"
               :create-if-needed true}))

```

List the content of a container.

```Clojure

(list cnt)
;;=> ("file1.txt" "file2.txt" "directory1/" ... ) 

```

List all blobs which starts with a common prefix

```Clojure

(list cnt :prefix "file")
;;=> ("file1.txt" "file2.txt")

```

List the content of the sub-directories
*NOTE: `list` is NOT lazy, so if you have large number of files might cause OutOfMemoryError.*

```Clojure

(list cnt :recursive true)
;;=> ("file1.txt" 
      "file2.txt" 
      "directory1/" 
      "directory1/fileA.txt" 
      "directory1/fileB.txt"
      ... ) 

```

Optionally you can get the full metadata associated with the blobs


```Clojure

(list cnt :recursive true :with-metadata true)

;; =>
({:is-directory? false,
  :path "directory1/fileA.txt",
  :uri #<URI https://my-account.blob.core.windows.net/my-container/directory1/fileA.txt>,
  :metadata {"hdi_permission" 
              "{\"owner\":\"admin\",\"group\":\"supergroup\",\"permissions\":\"rwxr-xr-x\"}" },
  :properties {:content-language nil,
               :content-disposition nil,
               :lease-status :UNLOCKED,
               :etag "\"0x8D1C0AF11753D19\"",
               :last-modified #inst "2014-10-28T10:14:18.000-00:00",
               :cache-control nil,
               :lease-duration nil,
               :length 544,
               :content-type "application/octet-stream",
               :content-md5 "Qtg4wMzVyJnihlpd2P6Msw==",
               :copy-state nil,
               :content-econding nil,
               :lease-state :AVAILABLE,
               :blob-type :BLOCK_BLOB}},
   
   ...)

```

For example if you want to find last 10 files which have been created/modified 
in a specific container you can do as follow:

```Clojure

; download last 10 files
(->>
 (list cnt :recursive true  :with-metadata true)       ;; list blobs with metadata
 (filter (complement :is-directory?))                  ;; filter directories out
 (map (juxt :path (comp :last-modified :properties)))  ;; extract only :path and :last-modified
 (sort-by second)                                      ;; sort-by timestamp
 (map first)                                           ;; extract only path names 
 (take-last 10))                                       ;; finally take the last 10

;=> ("new-file1.txt" "/some-dir/new-file2.json" ...)

```

You can download a blob with the following function which takes

  - a container
  - a blob to download
  - a local filename to write to
  - optionally whether to overwrite the file if it exists locally (default: false) 

```Clojure

(download-blob cnt "my-data/history/buildings.csv" "/tmp/buildings.csv" :overwrite true)
;;=> #<File "/tmp/buildings.csv">

```

If you don't mind about the local filename you can just download to a temporary file with:

```Clojure

(download-blob-to-temp
  cnt                              ;; a container
  "my-data/history/buildings.csv"  ;; blob to download
  "/tmp"                           ;; folder where to download to
  :file-prefix "XXXX"              ;; generate temp files with the following prefix (default "blob-") 
  :file-suffix "YYYY")             ;; generate temp files with the following suffiz (default ".data")


```

You can upload a file into blob storage using the following function which takes

  - a container
  - a blob name
  - a local filename to upload
  
```Clojure

(upload-blob cnt "Flats.csv" "/tmp/flats.csv")

```

## License

Copyright © 2014 B. Bonacci, D. Oliyide

Distributed under the The MIT License (MIT).
