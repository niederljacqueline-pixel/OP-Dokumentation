package at.fh.opdoc.repo;

import at.fh.opdoc.model.OpKatalogEintrag;
import java.util.List;

public interface OpKatalogRepository {
    List<OpKatalogEintrag> findAll();
    List<OpKatalogEintrag> search(String q);
    OpKatalogEintrag save(OpKatalogEintrag op);
    void deleteById(Integer id);
}