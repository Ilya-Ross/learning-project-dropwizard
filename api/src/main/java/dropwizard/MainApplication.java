package dropwizard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ilya.service.exception.AlreadyExistExceptionMapper;
import com.ilya.service.exception.NotFoundExceptionMapper;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import com.ilya.service.service.kafka.KafkaConsumer;

import javax.ws.rs.Path;
import java.util.Map;

public class MainApplication extends Application<BasicConfiguration> {

    private BasicConfiguration configuration;

    private Environment environment;

    private AnnotationConfigWebApplicationContext context;

    public static void main(String[] args) throws Exception {
        new MainApplication().run(args);
    }

    @Override
    public void run(BasicConfiguration configuration, Environment environment) {
        this.configuration = configuration;
        this.environment = environment;

        setUpSpringContext();
        registerResources();
        startConsumers();
    }

    private void startConsumers() {
        KafkaConsumer kafkaconsumer = context.getBean("kafkaconsumer", KafkaConsumer.class);
        Thread thread = new Thread(kafkaconsumer);
        thread.start();
    }

    private void setUpSpringContext() {
        AnnotationConfigWebApplicationContext parent = new AnnotationConfigWebApplicationContext();
        parent.refresh();

        parent.registerShutdownHook();
        parent.getBeanFactory().registerSingleton("configuration", configuration);
        parent.getBeanFactory().registerSingleton("applicationEnvironment", environment);
        parent.start();

        context = new AnnotationConfigWebApplicationContext();
        context.setParent(parent);
        context.register(ApplicationSpringConfiguration.class);
        context.refresh();
        context.registerShutdownHook();
        context.start();
    }

    private void registerResources() {
        Map<String, Object> beansWithAnnotation = context.getBeansWithAnnotation(Path.class);

        for (String beanName : beansWithAnnotation.keySet()) {
            Object resource = beansWithAnnotation.get(beanName);
            environment.jersey().register(resource);
        }
        environment.jersey().register(new AlreadyExistExceptionMapper());
        environment.jersey().register(new NotFoundExceptionMapper());
    }

    @Override
    public void initialize(Bootstrap<BasicConfiguration> bootstrap) {
        bootstrap.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        bootstrap.addBundle(new AssetsBundle("/apidocs", "/apidocs", "index.html"));
    }
}
