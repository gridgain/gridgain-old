/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.spi.indexing.h2.opt;

import org.apache.commons.codec.binary.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.apache.lucene.util.*;
import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.indexing.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.offheap.unsafe.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.spi.indexing.h2.GridH2IndexingSpi.*;

/**
 *
 */
public class GridLuceneIndex implements Closeable {
    /** */
    private final GridIndexingMarshaller marshaller;

    /** */
    private final String spaceName;

    /** */
    private final GridIndexingTypeDescriptor type;

    /** */
    private final IndexWriter writer;

    /** */
    private final String[] idxdFields;

    /** */
    private final boolean storeVal;

    /** */
    private final BitSet keyFields = new BitSet();

    /** */
    private final AtomicLong updateCntr = new GridAtomicLong();

    /** */
    private final GridLuceneDirectory dir;

    /**
     * Constructor.
     *
     * @param marshaller Indexing marshaller.
     * @param mem Unsafe memory.
     * @param spaceName Space name.
     * @param type Type descriptor.
     * @param storeVal Store value in index.
     * @throws GridSpiException If failed.
     */
    public GridLuceneIndex(GridIndexingMarshaller marshaller, @Nullable GridUnsafeMemory mem,
        @Nullable String spaceName, GridIndexingTypeDescriptor type, boolean storeVal) throws GridSpiException {
        this.marshaller = marshaller;
        this.spaceName = spaceName;
        this.type = type;
        this.storeVal = storeVal;

        dir = new GridLuceneDirectory(mem == null ? new GridUnsafeMemory(0) : mem);

        try {
            writer = new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_30, new StandardAnalyzer(
                Version.LUCENE_30)));
        }
        catch (IOException e) {
            throw new GridSpiException(e);
        }

        GridIndexDescriptor idx = null;

        for (GridIndexDescriptor descriptor : type.indexes().values()) {
            if (descriptor.type() == GridIndexType.FULLTEXT) {
                idx = descriptor;

                break;
            }
        }

        if (idx != null) {
            Collection<String> fields = idx.fields();

            idxdFields = new String[fields.size() + 1];

            fields.toArray(idxdFields);

            for (int i = 0, len = fields.size() ; i < len; i++)
                keyFields.set(i, type.keyFields().containsKey(idxdFields[i]));
        }
        else {
            assert type.valueTextIndex() || type.valueClass() == String.class;

            idxdFields = new String[1];
        }

        idxdFields[idxdFields.length - 1] = VAL_STR_FIELD_NAME;
    }

    /**
     * Stores given data in this fulltext index.
     *
     * @param key Key.
     * @param val Value.
     * @param ver Version.
     * @param expires Expiration time.
     * @throws GridSpiException If failed.
     */
    public void store(GridIndexingEntity<?> key, GridIndexingEntity<?> val, byte[] ver, long expires)
        throws GridSpiException {
        Document doc = new Document();

        Object k = key.value();
        Object v = val.value();

        boolean stringsFound = false;

        if (type.valueTextIndex() || type.valueClass() == String.class) {
            doc.add(new Field(VAL_STR_FIELD_NAME, v.toString(), Field.Store.YES, Field.Index.ANALYZED));

            stringsFound = true;
        }

        for (int i = 0, last = idxdFields.length - 1; i < last; i++) {
            Object fieldVal = type.value(keyFields.get(i) ? k : v, idxdFields[i]);

            if (fieldVal != null) {
                doc.add(new Field(idxdFields[i], fieldVal.toString(), Field.Store.YES, Field.Index.ANALYZED));

                stringsFound = true;
            }
        }

        if (!stringsFound)
            return; // We did not find any strings to be indexed, will not store data at all.

        doc.add(new Field(KEY_FIELD_NAME, Base64.encodeBase64String(marshaller.marshal(key)), Field.Store.YES,
            Field.Index.NOT_ANALYZED));

        if (storeVal && type.valueClass() != String.class)
            doc.add(new Field(VAL_FIELD_NAME, marshaller.marshal(val)));

        doc.add(new Field(VER_FIELD_NAME, ver));

        doc.add(new Field(EXPIRATION_TIME_FIELD_NAME, DateTools.timeToString(expires,
            DateTools.Resolution.MILLISECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));

        try {
            writer.addDocument(doc);

            updateCntr.incrementAndGet();
        }
        catch (IOException e) {
            throw new GridSpiException(e);
        }
    }

    /**
     * Removes entry for given key from this index.
     *
     * @param key Key.
     * @throws GridSpiException If failed.
     */
    public void remove(GridIndexingEntity<?> key) throws GridSpiException {
        try {
            writer.deleteDocuments(new Term(KEY_FIELD_NAME, Base64.encodeBase64String(marshaller.marshal(key))));

            updateCntr.incrementAndGet();
        }
        catch (IOException e) {
            throw new GridSpiException(e);
        }
    }

    /**
     * Runs lucene fulltext query over this index.
     *
     * @param qry Query.
     * @param filters Filters over result.
     * @return Query result.
     * @throws GridSpiException If failed.
     */
    public <K, V> GridCloseableIterator<GridIndexingKeyValueRow<K, V>> query(String qry,
        GridIndexingQueryFilter<K, V>[] filters) throws GridSpiException {
        IndexReader reader;

        try {
            long updates = updateCntr.get();

            if (updates != 0) {
                writer.commit();

                updateCntr.addAndGet(-updates);
            }

            reader = IndexReader.open(writer, true);
        }
        catch (IOException e) {
            throw new GridSpiException(e);
        }

        IndexSearcher searcher = new IndexSearcher(reader);

        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_30, idxdFields,
            writer.getAnalyzer());

        // Filter expired items.
        Filter f = new TermRangeFilter(EXPIRATION_TIME_FIELD_NAME, DateTools.timeToString(U.currentTimeMillis(),
            DateTools.Resolution.MILLISECOND), null, false, false);

        TopDocs docs;

        try {
            docs = searcher.search(parser.parse(qry), f, Integer.MAX_VALUE);
        }
        catch (Exception e) {
            throw new GridSpiException(e);
        }

        return new It<>(reader, searcher, docs.scoreDocs, filters);
    }

    /** {@inheritDoc} */
    @Override public void close() {
        U.closeQuiet(writer);
        U.closeQuiet(dir);
    }

    /**
     * Key-value iterator over fulltext search result.
     */
    private class It<K, V> extends GridCloseableIteratorAdapter<GridIndexingKeyValueRow<K, V>> {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final IndexReader reader;

        /** */
        private final IndexSearcher searcher;

        /** */
        private final ScoreDoc[] docs;

        /** */
        private final GridIndexingQueryFilter<K, V>[] filters;

        /** */
        private int idx;

        /** */
        private GridIndexingKeyValueRow<K, V> curr;

        /**
         * Constructor.
         *
         * @param reader Reader.
         * @param searcher Searcher.
         * @param docs Docs.
         * @param filters Filters over result.
         * @throws GridSpiException if failed.
         */
        private It(IndexReader reader, IndexSearcher searcher, ScoreDoc[] docs, GridIndexingQueryFilter<K, V>[] filters)
            throws GridSpiException {
            this.reader = reader;
            this.searcher = searcher;
            this.docs = docs;
            this.filters = filters;

            findNext();
        }

        /**
         * Filters key using predicates.
         *
         * @param key Key.
         * @param val Value.
         * @return {@code True} if key passes filter.
         */
        private boolean filter(K key, V val) {
            if (!F.isEmpty(filters)) {
                for (GridIndexingQueryFilter<K, V> f : filters) {
                    if (!f.apply(spaceName, key, val))
                        return false;
                }
            }

            return true;
        }

        /**
         * Finds next element.
         *
         * @throws GridSpiException If failed.
         */
        private void findNext() throws GridSpiException {
            curr = null;

            while (idx < docs.length) {
                Document doc;

                try {
                    doc = searcher.doc(docs[idx++].doc);
                }
                catch (IOException e) {
                    throw new GridSpiException(e);
                }

                String keyStr = doc.get(KEY_FIELD_NAME);

                GridIndexingEntity<K> k = marshaller.unmarshal(Base64.decodeBase64(keyStr));

                byte[] valBytes = doc.getBinaryValue(VAL_FIELD_NAME);

                GridIndexingEntity<V> v = valBytes != null ? marshaller.<V>unmarshal(valBytes) :
                    type.valueClass() == String.class ?
                    new GridIndexingEntityAdapter<>((V)doc.get(VAL_STR_FIELD_NAME), null): null;

                if (!filter(k.value(), v == null ? null : v.value()))
                    continue;

                byte[] ver = doc.getBinaryValue(VER_FIELD_NAME);

                curr = new GridIndexingKeyValueRowAdapter<>(k, v, ver);

                break;
            }
        }

        /** {@inheritDoc} */
        @Override protected GridIndexingKeyValueRow<K, V> onNext() throws GridException {
            GridIndexingKeyValueRow<K, V> res = curr;

            findNext();

            return res;
        }

        /** {@inheritDoc} */
        @Override protected boolean onHasNext() throws GridException {
            return curr != null;
        }

        /** {@inheritDoc} */
        @Override protected void onClose() throws GridException {
            U.closeQuiet(searcher);
            U.closeQuiet(reader);
        }
    }
}
