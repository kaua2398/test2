package com.valeshop.timesheet.services;

import com.valeshop.timesheet.entities.demands.DemandRecord;
import com.valeshop.timesheet.entities.demands.DemandRegisterDTO;
import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.exceptions.DemandNotFoundExeption;
import com.valeshop.timesheet.repositories.DemandRepository;
import com.valeshop.timesheet.schemas.DemandRegisterSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DemandService {

    @Autowired
    DemandRepository demandRepository;

    public List<DemandRecord> getAllDemandRecord() {
        return demandRepository.findAll();
    }

    public List<DemandRecord> getUserAllDemandRecord(Long id) {
        return demandRepository.findAllByUserId(id);
    }

    public DemandRecord registerDemand(DemandRegisterDTO dataUser, User user) {
        DemandRecord newDemand = new DemandRecord(
                null,
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

    public DemandRecord updateProblemObservationOrComment(DemandRegisterSchema registerData, int index, Long demandId, Long userId){
        DemandRecord demandRecord = demandRepository.findByIdAndUserId(demandId, userId)
                .orElseThrow(DemandNotFoundExeption::new);

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
        DemandRecord demandRecord = demandRepository.findByIdAndUserId(demandId, userId)
                .orElseThrow(DemandNotFoundExeption::new);

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

