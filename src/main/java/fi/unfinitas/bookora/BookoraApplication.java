package fi.unfinitas.bookora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BookoraApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookoraApplication.class, args);
	}

}
