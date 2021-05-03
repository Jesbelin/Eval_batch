package com.ipiecoles.batch.dbexport;

import com.ipiecoles.batch.model.Commune;
import com.ipiecoles.batch.repository.CommuneRepository;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.Writer;

public class FooterCallback implements FlatFileFooterCallback {
    //private final long count;

    @Autowired
    public CommuneRepository communeRepository;

    public FooterCallback(CommuneRepository communeRepository) {
        this.communeRepository = communeRepository;
    }

    @Override
    public void writeFooter(Writer writer) throws IOException {
        writer.write("Total communes : " + communeRepository.countDistinctCodePostal());
    }
}
