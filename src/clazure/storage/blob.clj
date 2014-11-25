(ns clj4azure.storage.blob
  (:require [clojure.tools.logging :as log])
  (:require [clojure.java.io :as io])
  (:import [com.microsoft.azure.storage CloudStorageAccount]
           [com.microsoft.azure.storage.blob CloudBlobClient
            CloudBlobContainer ListBlobItem BlobListingDetails
            CloudBlob BlobProperties LeaseDuration LeaseState
            LeaseStatus CopyState CloudBlobDirectory]
           [java.io File]))


(defn blob-container
  [{:keys [storage-account-name storage-account-key storage-container endpoint-protocol]
    :or {endpoint-protocol "https"}}]

  (let [connection-string (str "DefaultEndpointsProtocol=" endpoint-protocol ";AccountName=" storage-account-name ";AccountKey=" storage-account-key)
        storage-account (CloudStorageAccount/parse connection-string)
        blob-client (.createCloudBlobClient storage-account)
        blob-container (.getContainerReference blob-client storage-container)]
    (if (.exists blob-container)
      blob-container
      (log/error "The BlobContainer [" blob-container "] doesn't exist"))))


(defn download-blob
  "Takes the BlobContainer, blobName and a fullname of a file to download to.
   It returns a java.io.File, if it successfully downloads the blob"
  [^CloudBlobContainer container blob-name local-file
   & {:keys [overwrite] :or {overwrite true}}]
  (let [block-blob (.getBlockBlobReference container blob-name )]
    (if (.exists block-blob)
      (if (and (.exists (io/file local-file)) (not overwrite))
        (log/warn "File" local-file "already exists, cannot overwrite.")
        ;; else
        (do
          ;; ensure directory exists
          (let [dirs (.getParentFile (io/file local-file))]
            (when-not (.exists dirs)
              (.mkdirs dirs)))
          ;; download blob
          (with-open [output (io/output-stream local-file)]
              (.download block-blob output))
            (log/info "Downloaded blob[" blob-name "] as " local-file)
            (io/file local-file)))
      (log/error "The Blob [" blob-name "] does NOT exist in azure storage"))))


(defn download-blob-to-temp
  "Takes the BlobContainer, blobName and directory to download to.
   It returns a java.io.File, if it successfully downloads the blob.
   Optionally you can set :file-prefix and :file-suffix"
  [^CloudBlobContainer container blob-name storage-dir
   & {:keys [file-prefix file-suffix] :or {file-prefix "blob-" file-suffix ".data"}}]
  (let [downloaded-file (File/createTempFile file-prefix file-suffix (io/file storage-dir))]
    (download-blob container blob-name downloaded-file)))


(defprotocol ToClojure
  (->clj [data] "Transform the data into a clojure data structure"))


(defn- simple-path [^java.net.URI uri]
  (when uri
    (.replaceFirst (.getPath uri) "^/[^/]+/" "")))

(extend-protocol ToClojure
  nil
  (->clj [_] nil)

  CloudBlob
  (->clj [b]
    (let [uri (.getUri b)]
      (->
       {:path (simple-path uri) :uri uri :metadata (.getMetadata b) :properties (->clj (.getProperties b))}
       (#(assoc % :is-directory? (= "true" (get-in % [:metadata "hdi_isfolder"] "false")))))))


  CloudBlobDirectory
  (->clj [b]
    (let [uri (.getUri b)]
      {:path (simple-path uri) :uri uri :is-directory? true}))

  BlobProperties
  (->clj [prop]
     {
      :blob-type           (->clj (.getBlobType prop))
      :cache-control       (.getCacheControl prop)
      :content-disposition (.getContentDisposition prop)
      :content-econding    (.getContentEncoding prop)
      :content-language    (.getContentLanguage prop)
      :content-md5         (.getContentMD5 prop)
      :content-type        (.getContentType prop)
      :copy-state          (->clj (.getCopyState prop))
      :etag                (.getEtag prop)
      :last-modified       (.getLastModified prop)
      :lease-duration      (->clj (.getLeaseDuration prop))
      :lease-state         (->clj (.getLeaseState prop))
      :lease-status        (->clj (.getLeaseStatus prop))
      :length              (.getLength prop)
      })

  Enum
  (->clj [data] (keyword (.name data)))

  CopyState
  (->clj [data]
    {
     :bytes-copied (.getBytesCopied data)
     :total-bytes  (.getTotalBytes data)
     :status-description (.getStatusDescription data)
     :copy-id (.getCopyId data)
     :source (.getSource data)
     :status (->clj (.getStatus data))
     }))



(defn list
  "Lists the content of a Azure blob container.
   available options:

     :prefix \"a-file-prefix\" - returns only blobs which starts with this prefix
     :recursive true|false - whether to walk the tree or not (default false)
     :with-metadata true|false - whether or not returning associated :properties
                                  and :metadata (default false)
  "
  [cnt & {:keys [prefix recursive with-metadata]
          :or {prefix nil recursive false with-metadata false}}]
  (let [simple-response (comp :path ->clj)
        full-response   ->clj]
    (->> (.listBlobs cnt prefix recursive
                     (when with-metadata (java.util.EnumSet/of BlobListingDetails/METADATA))
                     nil nil)
         (map (if with-metadata full-response simple-response)))))
