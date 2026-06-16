package com.devcrafter.Patisserie.App.security.config;

import com.devcrafter.Patisserie.App.enums.Role;
import com.devcrafter.Patisserie.App.models.Admin;
import com.devcrafter.Patisserie.App.models.DeliveryZone;
import com.devcrafter.Patisserie.App.models.TemplateMessage;
import com.devcrafter.Patisserie.App.repository.DeliveryZoneRepository;
import com.devcrafter.Patisserie.App.repository.TemplateMessageRepository;
import com.devcrafter.Patisserie.App.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Slf4j
public class DatabaseInitializer {

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository) {

        return args -> {
            if (!userRepository.existsByRole(Role.ROLE_ADMIN)) {
                Admin admin = new Admin();
                admin.setEmail("bassitou63@gmail.com");
                admin.setFirstname("Admin");
                admin.setLastname("Toure");
                admin.setRole(Role.ROLE_ADMIN);
                userRepository.save(admin);
            }
        };
    }

    @Bean
    CommandLineRunner initTemplates(
            TemplateMessageRepository templateRepository) {
        return args -> {
            if (templateRepository.count() > 0) {
                log.info("Templates already exist — skipping");
                return;
            }

            log.info("Initializing WhatsApp templates...");

            createTemplate(templateRepository,
                    "CONFIRMATION",
                    "Confirmation de commande",
                    "Bonjour {Prenom}, votre commande n°{Numero} " +
                            "a bien été confirmée pour le {DateLivraison}. " +
                            "Montant total : {MontantTotal} FCFA. " +
                            "Acompte requis : {Acompte} FCFA. Merci !"
            );

            createTemplate(templateRepository,
                    "COMMANDE_PRETE",
                    "Commande prête",
                    "Bonjour {Prenom}, votre commande n°{Numero} " +
                            "est prête ! La livraison est prévue " +
                            "le {DateLivraison}. À bientôt !"
            );

            createTemplate(templateRepository,
                    "RAPPEL_PAIEMENT",
                    "Rappel acompte",
                    "Bonjour {Prenom}, nous vous rappelons que " +
                            "l'acompte de {Acompte} FCFA pour votre " +
                            "commande n°{Numero} n'a pas encore été reçu. " +
                            "Merci de nous contacter."
            );

            createTemplate(templateRepository,
                    "REMERCIEMENT",
                    "Remerciement post-livraison",
                    "Merci {Prenom} pour votre confiance ! " +
                            "Nous espérons que vous avez apprécié. " +
                            "N'hésitez pas à nous recommander."
            );

            createTemplate(templateRepository,
                    "RELANCE_IMPAYE",
                    "Relance solde impayé",
                    "Bonjour {Prenom}, un solde de {Solde} FCFA " +
                            "reste dû pour votre commande n°{Numero}. " +
                            "Merci de régulariser. " +
                            "Contactez-nous au {TelephonePatisserie}."
            );

            log.info(" {} templates initialized",
                    templateRepository.count());
        };
    }

    @Bean
    CommandLineRunner initZones(DeliveryZoneRepository zoneRepository) {
        return args -> {
            if (zoneRepository.count() > 0) {
                log.info("Zones already initialized — skipping");
                return;
            }

            log.info("Initializing default delivery zones...");

            List<Object[]> defaultZones = List.of(
                    new Object[]{"Cotonou",    "Cadjehoun",        1000},
                    new Object[]{"Cotonou",    "Haie Vive",        1000},
                    new Object[]{"Cotonou",    "Akpakpa",          1500},
                    new Object[]{"Cotonou",    "Gbèdjromèdé",      1000},
                    new Object[]{"Cotonou",    "Agla",             1200},
                    new Object[]{"Calavi",     "Godomey Salamey",  1500},
                    new Object[]{"Calavi",     "Godomey Togoudo",  1500},
                    new Object[]{"Calavi",     "Cocotomey",        2000},
                    new Object[]{"Calavi",     "Kpanroun",         2000},
                    new Object[]{"Calavi",     "Ouèdo",            2500},
                    new Object[]{"Porto-Novo", "Akron",            2500},
                    new Object[]{"Porto-Novo", "Akpakpa",          2500},
                    new Object[]{"Ouidah",     "Centre-ville",     3000},
                    new Object[]{"Sèmè-Podji", "Centre",           3500}
            );

            for (Object[] z : defaultZones) {
                DeliveryZone zone = new DeliveryZone();
                zone.setName((String) z[0]);
                zone.setNeighborhood((String) z[1]);
                zone.setDeliveryFees(BigDecimal.valueOf((Integer) z[2]));
                zone.setActif(true);
                zoneRepository.save(zone);
            }

            log.info("✅ {} delivery zones initialized", zoneRepository.count());
        };
    }

    private void createTemplate(
            TemplateMessageRepository repo,
            String type,
            String libelle,
            String contenu) {
        TemplateMessage t = new TemplateMessage();
        t.setType(type);
        t.setLibelle(libelle);
        t.setContent(contenu);
        repo.save(t);
    }
}
