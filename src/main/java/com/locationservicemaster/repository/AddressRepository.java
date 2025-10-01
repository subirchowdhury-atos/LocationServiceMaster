package com.locationservicemaster.repository;

import com.locationservicemaster.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    Optional<Address> findByStreetAddressAndCityAndStateAndZipCode(
            String streetAddress, String city, String state, String zipCode);
    
    List<Address> findByZipCode(String zipCode);
    
    List<Address> findByCityAndState(String city, String state);
    
    @Query("SELECT a FROM Address a WHERE a.isEligible = :eligible")
    List<Address> findByEligibility(@Param("eligible") Boolean eligible);
    
    @Query(value = "SELECT * FROM addresses WHERE " +
           "latitude BETWEEN :minLat AND :maxLat AND " +
           "longitude BETWEEN :minLon AND :maxLon", nativeQuery = true)
    List<Address> findByCoordinatesRange(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon);
    
    @Query("SELECT COUNT(a) > 0 FROM Address a WHERE " +
           "a.streetAddress = :streetAddress AND " +
           "a.city = :city AND " +
           "a.state = :state AND " +
           "a.zipCode = :zipCode AND " +
           "a.isEligible IS NOT NULL")
    boolean existsWithEligibilityCheck(
            @Param("streetAddress") String streetAddress,
            @Param("city") String city,
            @Param("state") String state,
            @Param("zipCode") String zipCode);
}