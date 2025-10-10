package com.valeshop.timesheet.services;

import com.valeshop.timesheet.entities.demands.DemandRecord;
import com.valeshop.timesheet.entities.demands.DemandRegisterDTO;
import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.entities.user.UserType;
import com.valeshop.timesheet.exceptions.DemandNotFoundExeption;
import com.valeshop.timesheet.exceptions.UserNotFoundException;
import com.valeshop.timesheet.repositories.DemandRepository;
import com.valeshop.timesheet.repositories.UserRepository;
import com.valeshop.timesheet.schemas.DemandRegisterSchema;
import com.valeshop.timesheet.schemas.DemandUpdateSchema;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class DemandService {

    @Autowired
    DemandRepository demandRepository;

    @Autowired
    UserRepository userRepository;

    public List<DemandRecord> getAllDemandRecord() {
        return demandRepository.findAll();
    }

    public List<DemandRecord> getUserAllDemandRecord(Long id) {
        return demandRepository.findAllByUserId(id);
    }

    private static String getUsernameFromEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        int atIndex = email.indexOf('@');

        if (atIndex != -1) {
            return email.substring(0, atIndex);
        } else {
            return email;
        }
    }
    public DemandRecord registerDemand(DemandRegisterDTO dataUser, User user) {
        String owner = getUsernameFromEmail(user.getEmail());

        DemandRecord newDemand = new DemandRecord(
                null,
                owner,
                dataUser.title(),
                dataUser.gitlink(),
                dataUser.priority(),
                dataUser.status(),
                dataUser.date(),
                dataUser.description(),
                user
        );

        return demandRepository.save(newDemand);
    }

    private <T> void updateFieldIfNotNull(T newValue, Consumer<T> setter) {
        if (newValue == null) {
            return;
        }

        if (newValue instanceof String && ((String) newValue).isBlank()) {
            return;
        }
        setter.accept(newValue);
    }

    @Transactional
    public DemandRecord demandUpdate(DemandUpdateSchema demandSchema, Long demandId, User currentUser) {
        DemandRecord demand = demandRepository.findById(demandId)
                .orElseThrow(DemandNotFoundExeption::new);

        // Lógica de atualização do dono da demanda
        if (demandSchema.getUserId() != null && !Objects.equals(demand.getUser().getId(), demandSchema.getUserId())) {
            if (currentUser.getUserType() != UserType.Administrador) {
                throw new AccessDeniedException("Apenas administradores podem reatribuir demandas.");
            }
            User newOwner = userRepository.findById(demandSchema.getUserId())
                    .orElseThrow(() -> new UserNotFoundException("Novo usuário responsável não encontrado."));
            demand.setUser(newOwner);
            demand.setOwner(getUsernameFromEmail(newOwner.getEmail()));
        }

        updateFieldIfNotNull(demandSchema.getTitle(), demand::setTitle);
        updateFieldIfNotNull(demandSchema.getGitLink(), demand::setGitLink);
        updateFieldIfNotNull(demandSchema.getPriority(), demand::setPriority);

        String originalStatus = demand.getStatus();
        String newStatus = demandSchema.getStatus();
        if (newStatus != null && !newStatus.equals(originalStatus)) {
            if ("Concluída".equalsIgnoreCase(newStatus)) {
                demand.setCompletionDate(new Date());
            } else if ("Concluída".equalsIgnoreCase(originalStatus)) {
                demand.setCompletionDate(null);
            }
            demand.setStatus(newStatus);
        }

        updateFieldIfNotNull(demandSchema.getDate(), demand::setDate);
        updateFieldIfNotNull(demandSchema.getDescription(), demand::setDescription);

        return demandRepository.save(demand);
    }


    public DemandRecord findDemandById(Long demandId) {
        return demandRepository.findById(demandId).orElseThrow(DemandNotFoundExeption::new);
    }

    public DemandRecord registerProblemObservationOrComment(DemandRegisterSchema registerData, Long demandId) {
        DemandRecord demandRecord = demandRepository.findById(demandId)
                .orElseThrow(DemandNotFoundExeption::new);

        if (registerData.getObservations() != null && !registerData.getObservations().isEmpty()) {
            List<String> existingObservations = demandRecord.getObservations();
            if (existingObservations == null) {
                existingObservations = new ArrayList<>();
            }
            existingObservations.addAll(registerData.getObservations());
            demandRecord.setObservations(existingObservations);
        }

        if (registerData.getProblems() != null && !registerData.getProblems().isEmpty()) {
            List<String> existingProblems = demandRecord.getProblems();
            if (existingProblems == null) {
                existingProblems = new ArrayList<>();
            }
            existingProblems.addAll(registerData.getProblems());
            demandRecord.setProblems(existingProblems);
        }

        if (registerData.getComments() != null && !registerData.getComments().isEmpty()) {
            List<String> existingComments = demandRecord.getComments();
            if (existingComments == null) {
                existingComments = new ArrayList<>();
            }
            existingComments.addAll(registerData.getComments());
            demandRecord.setComments(existingComments);
        }

        return demandRecord;
    }

    private DemandRecord findDemandForModification(Long demandId, User currentUser) {
        if (currentUser.getUserType() == UserType.Administrador) {
            return demandRepository.findById(demandId)
                    .orElseThrow(DemandNotFoundExeption::new);
        } else {
            return demandRepository.findByIdAndUserId(demandId, currentUser.getId())
                    .orElseThrow(DemandNotFoundExeption::new);
        }
    }

    public DemandRecord updateProblemObservationOrComment(DemandRegisterSchema registerData, int index, Long demandId, Long userId) {
        User currentUser = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        DemandRecord demandRecord = findDemandForModification(demandId, currentUser);


        if (registerData.getObservations() != null && !registerData.getObservations().isEmpty()) {
            List<String> existingObservations = demandRecord.getObservations();

            if (index < 0 || index >= existingObservations.size()) {
                throw new IndexOutOfBoundsException();
            }

            String newObservationText = registerData.getObservations().getFirst();
            existingObservations.set(index, newObservationText);
        }

        if (registerData.getProblems() != null && !registerData.getProblems().isEmpty()) {
            List<String> existingProblems = demandRecord.getProblems();

            if (index < 0 || index >= existingProblems.size()) {
                throw new IndexOutOfBoundsException();
            }

            String newProblemText = registerData.getProblems().getFirst();
            existingProblems.set(index, newProblemText);
        }

        if (registerData.getComments() != null && !registerData.getComments().isEmpty()) {
            List<String> existingComments = demandRecord.getComments();

            if (index < 0 || index >= existingComments.size()) {
                throw new IndexOutOfBoundsException();
            }

            String newCommentText = registerData.getComments().getFirst();
            existingComments.set(index, newCommentText);
        }

        return demandRecord;
    }


    public void deleteProblem(int index, Long demandId, Long userId) {
        DemandRecord demandRecord = demandRepository.findByIdAndUserId(demandId, userId)
                .orElseThrow(DemandNotFoundExeption::new);

        List<String> existingProblems = demandRecord.getProblems();

        if (existingProblems == null || index < 0 || index >= existingProblems.size()) {
            throw new IndexOutOfBoundsException();
        }

        existingProblems.remove(index);
    }

    public void deleteObservation(int index, Long demandId, Long userId) {
        DemandRecord demandRecord = demandRepository.findByIdAndUserId(demandId, userId)
                .orElseThrow(DemandNotFoundExeption::new);

        List<String> existingObservations = demandRecord.getObservations();

        if (existingObservations == null || index < 0 || index >= existingObservations.size()) {
            throw new IndexOutOfBoundsException();
        }

        existingObservations.remove(index);
    }

    public void deleteComment(int index, Long demandId, Long userId) {
        User currentUser = userRepository.findById(userId).orElseThrow(RuntimeException::new);
        DemandRecord demandRecord = findDemandForModification(demandId, currentUser);

        List<String> existingComments = demandRecord.getComments();

        if (existingComments == null || index < 0 || index >= existingComments.size()) {
            throw new IndexOutOfBoundsException();
        }

        existingComments.remove(index);
    }


    public void deleteDemand(Long demandId) {
        demandRepository.deleteById(demandId);
    }
}

