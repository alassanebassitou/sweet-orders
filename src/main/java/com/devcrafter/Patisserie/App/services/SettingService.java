package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ConflictException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.DeliveryZoneRequest;
import com.devcrafter.Patisserie.App.dto.request.SettingRequest;
import com.devcrafter.Patisserie.App.dto.request.TemplateMessageRequest;
import com.devcrafter.Patisserie.App.dto.response.DeliveryZoneResponse;
import com.devcrafter.Patisserie.App.dto.response.SettingResponse;
import com.devcrafter.Patisserie.App.dto.response.TemplateMessageResponse;
import com.devcrafter.Patisserie.App.models.DeliveryZone;
import com.devcrafter.Patisserie.App.models.Settings;
import com.devcrafter.Patisserie.App.models.TemplateMessage;
import com.devcrafter.Patisserie.App.repository.DeliveryZoneRepository;
import com.devcrafter.Patisserie.App.repository.SettingsRepository;
import com.devcrafter.Patisserie.App.repository.TemplateMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingService {

    private final SettingsRepository settingsRepository;
    private final DeliveryZoneRepository zoneRepository;
    private final TemplateMessageRepository templateRepository;

    // ─── Paramètres ──────────────────────────────────────────

    public SettingResponse getSettings() {
        SettingResponse r = new SettingResponse();
        r.setNamePatisserie(get("nom_patisserie", "Ma Pâtisserie"));
        r.setWhatsappPhoneNumber(get("telephone_whatsapp", ""));
        r.setEmail(get("email", ""));
        r.setAddress(get("adresse", ""));
        r.setMinimumDelayHour(
                Integer.parseInt(get("delai_minimum_heures", "24"))
        );
        r.setDepositPercentage(
                Integer.parseInt(get("pourcentage_acompte", "50"))
        );
        r.setProductionOverloadThreshold(
                Integer.parseInt(get("seuil_surcharge_production", "5"))
        );
        return r;
    }

    public SettingResponse updateSettings(
            SettingRequest request) {

        if (request.getNamePatisserie() != null)
            set("nom_patisserie", request.getNamePatisserie());
        if (request.getWhatsappPhoneNumber() != null)
            set("telephone_whatsapp",
                    request.getWhatsappPhoneNumber());
        if (request.getEmail() != null)
            set("email", request.getEmail());
        if (request.getAddress() != null)
            set("adresse", request.getAddress());
        if (request.getMinimumDelayHour() != null)
            set("delai_minimum_heures",
                    request.getMinimumDelayHour().toString());
        if (request.getDepositPercentage() != null)
            set("pourcentage_acompte",
                    request.getDepositPercentage().toString());
        if (request.getProductionOverloadThreshold() != null)
            set("seuil_surcharge_production",
                    request.getProductionOverloadThreshold()
                            .toString());

        log.info("Paramètres updated");
        return getSettings();
    }

    // ─── Zones de livraison ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeliveryZoneResponse> getZones() {
        return zoneRepository.findAllByActifTrue()
                .stream()
                .map(DeliveryZoneResponse::from)
                .sorted(Comparator
                        .comparing(DeliveryZoneResponse::getName)
                        .thenComparing(DeliveryZoneResponse::getNeighborhood))
                .toList();
    }

    @Transactional
    public DeliveryZoneResponse createZone(DeliveryZoneRequest request) {

        zoneRepository
                .findByNameIgnoreCaseAndNeighborhoodIgnoreCase(
                        request.getName(), request.getNeighborhood()
                )
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "La zone " + request.getName() +
                                    " / " + request.getNeighborhood() +
                                    " existe déjà."
                    );
                });

        DeliveryZone zone = new DeliveryZone();
        zone.setName(request.getName());
        zone.setNeighborhood(request.getNeighborhood());
        zone.setDescription(request.getDescription());
        zone.setDeliveryFees(
                request.getDeliveryFees() != null
                        ? request.getDeliveryFees()
                        : BigDecimal.ZERO
        );
        zone.setActif(true);
        return DeliveryZoneResponse.from(
                zoneRepository.save(zone)
        );
    }

    @Transactional
    public DeliveryZoneResponse modifyZone(
            Long id, DeliveryZoneRequest request) {
        DeliveryZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", id));
        if (request.getName() != null)
            zone.setName(request.getName().trim());
        if (request.getNeighborhood() != null)
            zone.setNeighborhood(request.getNeighborhood().trim());
        if (request.getDescription() != null)
            zone.setDescription(request.getDescription());
        if (request.getDeliveryFees() != null)
            zone.setDeliveryFees(request.getDeliveryFees());
        return DeliveryZoneResponse.from(
                zoneRepository.save(zone)
        );
    }

    public void deleteZone(Long id) {
        DeliveryZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", id));
        zone.setActif(false);
        zoneRepository.save(zone);
    }

    // ─── Templates messages ───────────────────────────────────

    public List<TemplateMessageResponse> getTemplates() {
        return templateRepository.findAll()
                .stream()
                .map(TemplateMessageResponse::from)
                .toList();
    }

    public TemplateMessageResponse updateTemplate(
            Long id, TemplateMessageRequest request) {
        TemplateMessage template = templateRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Template", id));
        template.setContent(request.getContent());
        return TemplateMessageResponse.from(
                templateRepository.save(template)
        );
    }

    // ─── Helpers ─────────────────────────────────────────────

    private String get(String cle, String defaultValue) {
        return settingsRepository.findByCle(cle)
                .map(Settings::getValue)
                .orElse(defaultValue);
    }

    private void set(String cle, String valeur) {
        Settings p = settingsRepository
                .findByCle(cle)
                .orElseGet(() -> {
                    Settings newP = new Settings();
                    newP.setCle(cle);
                    return newP;
                });
        p.setValue(valeur);
        settingsRepository.save(p);
    }
}
