package org.apache.lucene.index;


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.ReaderUtil;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Exposes flex API, merged from flex API of
 * sub-segments.
 *
 * @lucene.experimental
 */

public final class MultiTerms extends Terms {
  private final Terms[] subs;
  private final ReaderUtil.Slice[] subSlices;
  private final BytesRef.Comparator termComp;

  public MultiTerms(Terms[] subs, ReaderUtil.Slice[] subSlices) throws IOException {
    this.subs = subs;
    this.subSlices = subSlices;
    
    BytesRef.Comparator _termComp = null;
    for(int i=0;i<subs.length;i++) {
      if (_termComp == null) {
        _termComp = subs[i].getComparator();
      } else {
        // We cannot merge sub-readers that have
        // different TermComps
        final BytesRef.Comparator subTermComp = subs[i].getComparator();
        if (subTermComp != null && !subTermComp.equals(_termComp)) {
          throw new IllegalStateException("sub-readers have different BytesRef.Comparators; cannot merge");
        }
      }
    }

    termComp = _termComp;
  }

  @Override
  public TermsEnum iterator() throws IOException {

    final List<MultiTermsEnum.TermsEnumIndex> termsEnums = new ArrayList<MultiTermsEnum.TermsEnumIndex>();
    for(int i=0;i<subs.length;i++) {
      final TermsEnum termsEnum = subs[i].iterator();
      if (termsEnum != null) {
        termsEnums.add(new MultiTermsEnum.TermsEnumIndex(termsEnum, i));
      }
    }

    if (termsEnums.size() > 0) {
      return new MultiTermsEnum(subSlices).reset(termsEnums.toArray(MultiTermsEnum.TermsEnumIndex.EMPTY_ARRAY));
    } else {
      return null;
    }
  }

  @Override
  public BytesRef.Comparator getComparator() {
    return termComp;
  }
}

