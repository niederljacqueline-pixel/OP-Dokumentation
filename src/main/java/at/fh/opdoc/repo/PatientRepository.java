package at.fh.opdoc.repo;

import at.fh.opdoc.model.Patient;
import java.util.List;

public interface PatientRepository {
    List<Patient> findAll();
    List<Patient> search(String q);
    Patient save(Patient p);      // insert/update
    void deleteById(Integer id);
}