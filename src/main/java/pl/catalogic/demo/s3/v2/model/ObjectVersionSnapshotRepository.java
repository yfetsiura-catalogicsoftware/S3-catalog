package pl.catalogic.demo.s3.v2.model;

import org.springframework.data.repository.CrudRepository;

public interface ObjectVersionSnapshotRepository
    extends CrudRepository<ObjectVersionSnapshot, String> {

}
