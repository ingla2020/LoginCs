package scm.csm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CsmApplication {

	public static void main(String[] args) {
		SpringApplication.run(CsmApplication.class, args);

		Csm csm = new Csm();
		csm.token();
	}

}
