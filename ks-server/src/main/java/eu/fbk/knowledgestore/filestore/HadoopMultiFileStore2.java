package eu.fbk.knowledgestore.filestore;

import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.io.ByteStreams;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fbk.knowledgestore.data.Data;
import eu.fbk.knowledgestore.data.Stream;

/**
 * A {@code FileStore} implementation based on the Hadoop API optimized for huge number of files.
 * <p>
 * An {@code HadoopFileStore} stores its files in an Hadoop
 * {@link org.apache.hadoop.fs.FileSystem}, under a certain, configurable root path; the
 * filesystem can be any of the filesystems supported by the Hadoop API, including the local (raw)
 * filesystem and the distributed HDFS filesystem.
 * </p>
 * <p>
 * Files are stored in a a two-level directory structure, where first level directories reflect
 * the MIME types of stored files, and second level directories are buckets of files whose name is
 * obtained by hashing the filename; buckets are used in order to equally split a large number of
 * files in several subdirectories, overcoming possible filesystem limitations in terms of maximum
 * number of files storable in a directory.
 * </p>
 */
public final class HadoopMultiFileStore2 implements FileStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(HadoopMultiFileStore2.class);

    private static final String DEFAULT_ROOT_PATH = "files";

    private static final String DEFAULT_LUCENE_PATH = "./lucene-index";

    private static final int DEFAULT_NUM_SMALL_FILES = 10;

    private static final long DEFAULT_CLEANUP_PERIOD = 10000L; // 5s

    private static final String SMALL_FILES_PATH = "_small";

    private static final int MAX_LUCENE_SEGMENTS = 100;

    private static final String KEY_FIELD = "filename";

    private static final String VALUE_FIELD = "zipfilename";

    private static final String DELETED = "__deleted";

    private final FileSystem fileSystem;

    private final Path rootPath;

    private final Path smallFilesPath;

    private final File luceneFolder;

    private final int numSmallFiles;

    private final long cleanupPeriod;

    private final Multiset<String> openedFiles;

    private final ReadWriteLock luceneLock;

    private IndexReader luceneReader;

    private IndexWriter luceneWriter;

    private Future<?> cleanupFuture;

    /**
     * Creates a new {@code HadoopFileStore} storing files in the {@code FileSystem} and under the
     * {@code rootPath} specified.
     *
     * @param fileSystem
     *            the file system, not null
     * @param path
     *            the root path where to store files, possibly relative to the filesystem working
     *            directory; if null, the default root path {@code files} will be used
     * @param numSmallFile
     *            the number of files to put in each zip file
     * @param cleanupPeriod
     *            the amount of time in milliseconds between cleanup operations
     */
    public HadoopMultiFileStore2(final FileSystem fileSystem, @Nullable final String lucenePath,
            @Nullable final String path, @Nullable final Integer numSmallFile,
            @Nullable final Long cleanupPeriod) {

        this.fileSystem = Preconditions.checkNotNull(fileSystem);
        this.luceneFolder = new File(MoreObjects.firstNonNull(lucenePath, DEFAULT_LUCENE_PATH));
        this.rootPath = new Path(MoreObjects.firstNonNull(path, DEFAULT_ROOT_PATH))
                .makeQualified(this.fileSystem); // resolve wrt workdir
        this.smallFilesPath = new Path(this.rootPath.toString() + File.separator
                + SMALL_FILES_PATH).makeQualified(this.fileSystem);
        this.numSmallFiles = numSmallFile != null ? numSmallFile : DEFAULT_NUM_SMALL_FILES;
        this.cleanupPeriod = cleanupPeriod != null ? cleanupPeriod : DEFAULT_CLEANUP_PERIOD;
        this.openedFiles = HashMultiset.create();
        this.luceneLock = new ReentrantReadWriteLock(true);
        LOGGER.info("{} configured, paths={};{}", getClass().getSimpleName(), this.rootPath,
                this.luceneFolder);
    }

    @Override
    public void init() throws IOException {

        // Create root folder if missing
        if (!this.fileSystem.exists(this.rootPath)) {
            LOGGER.debug("Creating root folder {}", this.rootPath);
            if (!this.fileSystem.mkdirs(this.rootPath)) {
                throw new IOException("Cannot create root folter " + this.luceneFolder);
            }
        }

        // Create sub-folder for small files, if missing
        if (!this.fileSystem.exists(this.smallFilesPath)) {
            LOGGER.debug("Creating small files folder {}", this.smallFilesPath);
            if (!this.fileSystem.mkdirs(this.smallFilesPath)) {
                throw new IOException("Cannot create small files folter " + this.smallFilesPath);
            }
        }

        // Create folder for lucene index, if missing
        if (!this.luceneFolder.exists()) {
            LOGGER.debug("Created lucene folder {}", this.luceneFolder);
            if (!this.luceneFolder.mkdirs()) {
                throw new IOException("Cannot create lucene folder " + this.luceneFolder);
            }
        }

        // Initialize Lucene writer and reader
        this.luceneWriter = new IndexWriter(FSDirectory.open(this.luceneFolder),
                new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
        this.luceneReader = this.luceneWriter.getReader();

        // Schedule periodic cleanup
        this.cleanupFuture = Data.getExecutor().scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    merge();
                    purge();
                    indexOptimize();
                } catch (final Throwable ex) {
                    LOGGER.warn("Periodic cleanup failed", ex);
                }
            }

        }, this.cleanupPeriod, this.cleanupPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public InputStream read(final String fileName) throws IOException {

        // Lookup the current zip file / deleted status for the file name supplied
        final String zipName = indexGet(fileName);

        // Proceed only if file is not marked as deleted
        if (!DELETED.equals(zipName)) {
            if (zipName != null) {
                // Search in zipped file
                final Path zipPath = pathForZipFile(zipName);
                try {
                    final ZipInputStream stream = new ZipInputStream(openForRead(zipPath));
                    ZipEntry entry;
                    while ((entry = stream.getNextEntry()) != null) {
                        if (entry.getName().equals(fileName)) {
                            LOGGER.debug("Reading {} from ZIP file {}", fileName, zipPath);
                            return stream;
                        }
                    }
                } catch (final IOException ex) {
                    throw new IOException("Cannot read " + fileName + " from ZIP file" + zipPath,
                            ex);
                }

            } else {
                // Search in small files
                final Path smallPath = pathForSmallFile(fileName);
                if (this.fileSystem.exists(smallPath)) {
                    LOGGER.debug("Reading small file {}", smallPath);
                    return openForRead(smallPath);
                }
            }
        }

        // Report missing file
        throw new FileMissingException(fileName, "The file does not exist");
    }

    @Override
    public OutputStream write(final String fileName) throws IOException {

        // Throw an exception in case a file with the same name already exists
        final String zipName = indexGet(fileName);
        final Path smallPath = pathForSmallFile(fileName);
        if (zipName != null && !DELETED.equals(zipName) || this.fileSystem.exists(smallPath)) {
            throw new FileExistsException(fileName, "Cannot overwrite file");
        }

        // Write small file
        LOGGER.debug("Creating small file {}", smallPath);
        return openForWrite(smallPath);
    }

    // Synchronization serves to (1) avoid the same ZIP file to be exploded multiple times and
    // (2) to delete files while merge is occurring

    @Override
    public synchronized void delete(final String fileName) throws FileMissingException,
            IOException {

        // Lookup the zip file / deleted status for the supplied file
        final String zipName = indexGet(fileName);

        // Proceed only if file is not marked as deleted
        if (!DELETED.equals(zipName)) {
            if (zipName != null) {

                // Explode the ZIP file (except for the deleted file)
                final Map<String, String> entries = new HashMap<>();
                final Path zipPath = pathForZipFile(zipName);
                LOGGER.debug("Exploding zip file {}", zipPath);
                try (final ZipInputStream zipStream = new ZipInputStream(openForRead(zipPath))) {
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {
                        final String smallName = entry.getName();
                        if (!smallName.equals(fileName)) {
                            entries.put(smallName, null);
                            final Path smallPath = pathForSmallFile(smallName);
                            try (OutputStream stream = openForWrite(smallPath)) {
                                ByteStreams.copy(zipStream, stream);
                            }
                        }
                    }
                } catch (final IOException ex) {
                    // Perform clean up and propagate exception
                    for (final String smallName : entries.keySet()) {
                        final Path smallPath = pathForSmallFile(smallName);
                        try {
                            this.fileSystem.delete(smallPath, false);
                        } catch (final Throwable ex2) {
                            LOGGER.warn("Could not delete extracted file " + smallPath
                                    + " after failed to explod ZIP file " + zipPath, ex2);
                        }
                    }
                    throw new IOException("Cannot explode ZIP file " + zipPath, ex);
                }

                // Update index, marking small file and ZIP as deleted and de-associating
                // other small files previously in the ZIP from the ZIP file
                entries.put(fileName, DELETED);
                entries.put(zipName, DELETED);
                indexPut(entries);
                return;

            } else {
                // Mark a small file with the name supplied as deleted
                final Path smallPath = pathForSmallFile(fileName);
                if (this.fileSystem.exists(smallPath)) {
                    LOGGER.debug("Marking small file {} as deleted", smallPath);
                    indexPut(ImmutableMap.of(fileName, DELETED));
                    return;
                }
            }
        }

        // Report file does not exist
        throw new FileMissingException(fileName, "The file does not exist");
    }

    @Override
    public Stream<String> list() throws IOException {

        // Retrieve small files
        final List<String> smallNames = new ArrayList<>();
        for (final FileStatus fs : this.fileSystem.listStatus(this.smallFilesPath)) {
            final String smallName = fs.getPath().getName();
            if (!DELETED.equals(indexGet(smallName))) {
                smallNames.add(smallName);
            }
        }

        // Retrieve an iterator over zipped files
        final Iterator<String> zippedNames = indexList(false);

        // Return the concatenation of the two
        return Stream.concat(Stream.create(smallNames), Stream.create(zippedNames));
    }

    @Override
    public void close() {

        try {
            // Stop periodic cleanup
            this.cleanupFuture.cancel(false);
        } catch (final Throwable ex) {
            LOGGER.warn("Unable to stop periodic cleanup task", ex);
        }

        try {
            // Close Lucene reader
            this.luceneReader.close();
        } catch (final Throwable ex) {
            LOGGER.warn("Unable to close Lucene reader", ex);
        }

        try {
            // Optimize Lucene writer before closing
            this.luceneWriter.optimize();
        } catch (final Exception ex) {
            LOGGER.warn("Unable to optimize Lucene writer", ex);
        }

        try {
            // Close Lucene writer
            this.luceneWriter.close();
        } catch (final Exception ex) {
            LOGGER.warn("Unable to close Lucene writer", ex);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private void purge() throws IOException {

        // Identify deleted files that can be safely purged (i.e., not opened)
        final List<String> purgableFiles = new ArrayList<>();
        synchronized (this.openedFiles) {
            for (final Iterator<String> i = indexList(true); i.hasNext();) {
                final String file = i.next();
                if (!this.openedFiles.contains(i)) {
                    purgableFiles.add(file);
                }
            }
        }

        // Delete purgable files
        final Map<String, String> entries = new HashMap<>();
        for (final String file : purgableFiles) {
            try {
                final Path smallPath = pathForSmallFile(file);
                if (this.fileSystem.exists(smallPath)) {
                    this.fileSystem.delete(smallPath, false);
                } else {
                    final Path zipPath = pathForZipFile(file);
                    if (this.fileSystem.exists(zipPath)) {
                        this.fileSystem.delete(zipPath, false);
                    } else {
                        LOGGER.warn("Cannot find file " + file);
                    }
                }
                entries.put(file, null);
            } catch (final Throwable ex) {
                LOGGER.warn("Cannot purge file " + file, ex);
            }
        }

        // Update index
        indexPut(entries);
    }

    @SuppressWarnings("unchecked")
    private synchronized void merge() throws IOException {

        // Retrieve the list of small files that can be packed in a zip file
        final List<FileStatus> list = new ArrayList<>(this.numSmallFiles);
        final FileStatus[] files = this.fileSystem.listStatus(this.smallFilesPath);
        if (files != null && files.length >= this.numSmallFiles) {
            for (final FileStatus fs : files) {
                if (!fs.isDir() && !DELETED.equals(indexGet(fs.getPath().getName()))) {
                    list.add(fs);
                    if (list.size() == this.numSmallFiles) {
                        break;
                    }
                }
            }
        }

        // Abort if there are not enough files to build the zip
        if (list.size() < this.numSmallFiles) {
            return;
        }

        // Determine the name of the zip file, by hashing the names of its small files
        Collections.sort(list);
        final StringBuilder builder = new StringBuilder();
        for (final FileStatus fs : list) {
            builder.append(fs.toString());
        }
        final String zipName = Data.hash(builder.toString());
        final Path zipPath = pathForZipFile(zipName);

        final Map<String, String> entries = new HashMap<>();
        try {
            // Try to build the zip file
            try (final ZipOutputStream out = new ZipOutputStream(openForWrite(zipPath))) {
                for (final FileStatus fs : list) {
                    final String fileName = fs.getPath().getName();
                    out.putNextEntry(new ZipEntry(fileName));
                    try (final InputStream in = openForRead(fs.getPath())) {
                        ByteStreams.copy(in, out);
                    }
                    entries.put(fileName, zipName);
                }
            }

            // Update index
            indexPut(entries);

        } catch (final Throwable ex) {
            // On failure, delete and unindex the zip file
            try {
                for (final Map.Entry<String, String> entry : entries.entrySet()) {
                    entry.setValue(null);
                }
                indexPut(entries);
            } catch (final Throwable ex2) {
                LOGGER.warn("Cannot unindex zip file after failure to generate it", ex2);
            }
            try {
                this.fileSystem.delete(zipPath, false);
            } catch (final Throwable ex2) {
                LOGGER.warn("Cannot delete zip file after failure to generate it", ex2);
            }
            throw new IOException("Cannot build and index zip file " + zipPath, ex);
        }
    }

    private void indexOptimize() throws IOException {
        if (!this.luceneReader.isOptimized()) {
            LOGGER.debug("Optimizing index");
            this.luceneWriter.optimize(MAX_LUCENE_SEGMENTS);
            this.luceneLock.writeLock().lock();
            try {
                this.luceneReader.close();
                this.luceneReader = this.luceneWriter.getReader();
            } finally {
                this.luceneLock.writeLock().unlock();
            }
        }
    }

    private String indexGet(final String key) throws IOException {
        this.luceneLock.readLock().lock();
        try {
            final Term s = new Term(KEY_FIELD, key);
            final TermDocs termDocs = this.luceneReader.termDocs(s);
            if (termDocs.next()) {
                final Document doc = this.luceneReader.document(termDocs.doc());
                return doc.get(VALUE_FIELD);
            }
            return null;
        } finally {
            this.luceneLock.readLock().unlock();
        }
    }

    private void indexPut(final Map<String, String> entries) throws IOException {
        try {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                if (entry.getValue() == null) {
                    this.luceneWriter.deleteDocuments(new Term(VALUE_FIELD, entry.getKey()));
                } else {
                    final Document doc = new Document();
                    doc.add(new Field(KEY_FIELD, entry.getKey(), Field.Store.YES,
                            Field.Index.NOT_ANALYZED));
                    doc.add(new Field(VALUE_FIELD, entry.getValue(), Field.Store.YES,
                            Field.Index.NOT_ANALYZED));
                    this.luceneWriter.updateDocument(new Term(KEY_FIELD, entry.getKey()), doc);
                }
            }
            this.luceneWriter.commit();
            this.luceneLock.writeLock().lock();
            try {
                this.luceneReader.close();
                this.luceneReader = this.luceneWriter.getReader();
            } finally {
                this.luceneLock.writeLock().unlock();
            }
        } catch (final Throwable ex) {
            throw new IOException("Failed to update Lucene index with entries " + entries, ex);
        }
    }

    private Iterator<String> indexList(final boolean deleted) throws IOException {

        if (deleted) {
            // Perform a direct lookup
            final List<String> deletedNames = new ArrayList<>();
            final Term s = new Term(VALUE_FIELD, DELETED);
            this.luceneLock.readLock().lock();
            try {
                final TermDocs termDocs = this.luceneReader.termDocs(s);
                while (termDocs.next()) {
                    deletedNames.add(this.luceneReader.document(termDocs.doc()).get(KEY_FIELD));
                }
            } finally {
                this.luceneLock.readLock().unlock();
            }
            return deletedNames.iterator();

        } else {
            // Iterate over the whole index
            return new AbstractIterator<String>() {

                private int maxIndex = -1;

                private int currentIndex = 0;

                @Override
                protected String computeNext() {
                    HadoopMultiFileStore2.this.luceneLock.readLock().lock();
                    try {
                        if (this.maxIndex < 0) {
                            this.maxIndex = HadoopMultiFileStore2.this.luceneReader.maxDoc();
                        }
                        while (this.currentIndex <= this.maxIndex) {
                            final Document document = HadoopMultiFileStore2.this.luceneReader
                                    .document(this.currentIndex++);
                            if (document != null && !DELETED.equals(document.get(VALUE_FIELD))) {
                                return document.get(HadoopMultiFileStore2.KEY_FIELD);
                            }
                        }
                    } catch (final Throwable ex) {
                        throw new RuntimeException("Error iterating over Lucene index", ex);
                    } finally {
                        HadoopMultiFileStore2.this.luceneLock.readLock().unlock();
                    }
                    return endOfData();
                }

            };
        }
    }

    private InputStream openForRead(final Path filePath) throws IOException {
        final String fileName = filePath.getName();
        final InputStream stream = this.fileSystem.open(filePath);
        synchronized (this.openedFiles) {
            this.openedFiles.add(fileName);
        }
        return new FilterInputStream(stream) {

            @Override
            public void close() throws IOException {
                try {
                    LOGGER.debug("Closing {}", fileName);
                    super.close();
                } finally {
                    HadoopMultiFileStore2.this.openedFiles.remove(fileName);
                }
            }

        };
    }

    private OutputStream openForWrite(final Path filePath) throws IOException {
        final String fileName = filePath.getName();
        final OutputStream stream = this.fileSystem.create(filePath, false);
        synchronized (this.openedFiles) {
            this.openedFiles.add(fileName);
        }
        return new FilterOutputStream(stream) {

            @Override
            public void close() throws IOException {
                try {
                    LOGGER.debug("Closing {}", fileName);
                    super.close();
                } finally {
                    HadoopMultiFileStore2.this.openedFiles.remove(fileName);
                }
            }

        };
    }

    @Nullable
    private Path pathForSmallFile(@Nullable final String smallFile) {
        return smallFile == null ? null : new Path(this.smallFilesPath, smallFile);
    }

    @Nullable
    private Path pathForZipFile(@Nullable final String zipFile) {
        if (zipFile == null) {
            return null;
        }
        final String bucketDirectory = zipFile.substring(0, 2);
        return new Path(this.rootPath, bucketDirectory + File.separator + zipFile);
    }

}
