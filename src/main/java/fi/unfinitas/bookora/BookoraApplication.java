package fi.unfinitas.bookora;

import fi.unfinitas.bookora.config.BookoraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class BookoraApplication {

	public static void main(final String[] args) {
		SpringApplication.run(BookoraApplication.class, args);
	}

}
