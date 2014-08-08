package com.lyncode.xoai.builders.dataprovider;


import com.lyncode.builder.Builder;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;

public class MetadataBuilder implements Builder<Metadata> {

    private Collection<Element> elements = new ArrayList<Element>();

    public MetadataBuilder withElement(Element elem) {
        this.elements.add(elem);
        return this;
    }

    public MetadataBuilder withElement(Element... elems) {
        this.elements.addAll(asList(elems));
        return this;
    }


    public Metadata build() {
        Metadata metadata = new Metadata();
        metadata.getElement().addAll(elements);
        return metadata;
    }
}
