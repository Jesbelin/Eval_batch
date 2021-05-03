package com.ipiecoles.batch.dbexport;

import com.ipiecoles.batch.model.Commune;
import com.ipiecoles.batch.repository.CommuneRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import javax.persistence.EntityManagerFactory;

@Configuration
public class CommunesDBExportBatch {
    @Value("${importFile.chunkSize}")
    private Integer chunkSize;

    @Autowired
    public EntityManagerFactory entityManagerFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    CommuneRepository communeRepository;

    @Bean
    public Tasklet communesExportTasklet(){
        return new CommunesExportTasklet();
    }

    @Bean
    public JpaPagingItemReader<Commune> myJpaReader() {
        return new JpaPagingItemReaderBuilder<Commune>()
                .name("myJpaReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(10)
                .queryString("from Commune c order by code_postal, code_insee")
                .build();
    }

    @Bean
    public ItemWriter<Commune> fileWriter() {
        BeanWrapperFieldExtractor<Commune> bwfe = new BeanWrapperFieldExtractor<Commune>();
        bwfe.setNames(new String[] {"codePostal", "codeInsee", "nom", "latitude", "longitude"});

        FormatterLineAggregator<Commune> agg = new FormatterLineAggregator<>();
        agg.setFormat("%5s - %5s - %s : %.5f %.5f");
        agg.setFieldExtractor(bwfe);

        FlatFileItemWriter<Commune> flatFileItemWriter = new FlatFileItemWriter<>();
        flatFileItemWriter.setName("txtWriter");
        flatFileItemWriter.setResource(new FileSystemResource("target/test.txt"));
        flatFileItemWriter.setLineAggregator(agg);
        flatFileItemWriter.setHeaderCallback(new HeaderCallback(communeRepository));
        flatFileItemWriter.setFooterCallback(new FooterCallback(communeRepository));

        return flatFileItemWriter;
    }

    @Bean
    public CommunesDBExportSkipListener communesDBExportSkipListener() {
        return new CommunesDBExportSkipListener();
    }

    @Bean
    public Step stepExportTxt(){
        return stepBuilderFactory.get("exportFile")
                .<Commune, Commune> chunk(chunkSize)
                .reader(myJpaReader())
                .writer(fileWriter())
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .listener(communesDBExportSkipListener())
                .build();
    }

    @Bean
    public Step stepExportTasklet(){
        return stepBuilderFactory.get("stepExportTasklet")
                .tasklet(communesExportTasklet())
                .listener(communesExportTasklet())
                .build();
    }

    @Bean
    @Qualifier("exportCommunes")
    public Job exportCommunes(Step stepExportTxt){
        return jobBuilderFactory.get("exportCommunes")
                .incrementer(new RunIdIncrementer())
                .flow(stepExportTasklet())
                .next(stepExportTxt)
                .end().build();
    }
}
