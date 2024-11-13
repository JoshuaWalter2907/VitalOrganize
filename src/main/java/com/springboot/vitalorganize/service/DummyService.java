package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dao.DummyDAO;
import com.springboot.vitalorganize.model.Dummy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DummyService {

    private final DummyDAO dummyDAO;

    @Autowired
    public DummyService(DummyDAO dummyDAO) {
        this.dummyDAO = dummyDAO;
    }

    // Findet einen Dummy-Eintrag per ID
    public Dummy getDummyById(int id) {
        return dummyDAO.findById(id);
    }

    // Gibt alle Dummy-Einträge zurück
    public List<Dummy> getAllDummies() {
        return dummyDAO.findAll();
    }

    // Fügt einen neuen Dummy-Eintrag hinzu
    public int addDummy(Dummy dummy) {
        return dummyDAO.save(dummy);
    }

    // Löscht einen Dummy-Eintrag per ID
    public int deleteDummyById(int id) {
        return dummyDAO.deleteById(id);
    }
}
