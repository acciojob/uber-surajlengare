package com.driver.services.impl;

import com.driver.model.*;
import com.driver.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;



	@Override
	public void register(Customer customer)
	{
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId)
	{
		// Delete customer without using deleteById function
		Customer customer = customerRepository2.findById(customerId).get();
		List<TripBooking> tripBookingList = customer.getTripBookingList();

		// check all trips of this customer. If status of these trips is CONFIRED then change it to CANCELLED
		for (TripBooking t: tripBookingList)
		{
			if (t.getTripStatus() == TripStatus.CONFIRMED)
			{
				t.setTripStatus(TripStatus.CANCELED);
			}
		}
		customerRepository2.delete(customer);
	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception
	{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query
		List<Driver> allDriversList = driverRepository2.findAll();
		List<Integer> availableIds = new ArrayList<>();
		for (Driver d: allDriversList)
		{
			if (d.getCab().isAvailable() == true)
			{
				availableIds.add(d.getDriverId());
			}
		}

		if (availableIds.size()==0)
		{
			throw new Exception("No cab available!");
		}

		Collections.sort(availableIds);
		int driver_id = availableIds.get(0);

		Customer customer = customerRepository2.findById(customerId).get();
		Driver driver = driverRepository2.findById(driver_id).get();

		TripBooking tripBooking = new TripBooking();

		tripBooking.setFromLocation(fromLocation);
		tripBooking.setToLocation(toLocation);
		tripBooking.setDistanceInKm(distanceInKm);
		tripBooking.setTripStatus(TripStatus.CONFIRMED);
		tripBooking.setCustomer(customer);
		tripBooking.setDriver(driver);
		int rate = driver.getCab().getPerKmRate();
		tripBooking.setBill(distanceInKm * rate);

		tripBookingRepository2.save(tripBooking);

		driver.getCab().setAvailable(false);

		customer.getTripBookingList().add(tripBooking);
		customerRepository2.save(customer);

		driver.getTripBookingList().add(tripBooking);
		driverRepository2.save(driver);

		return tripBooking;
	}

	@Override
	public void cancelTrip(Integer tripId)
	{
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly

		TripBooking tripBooking = tripBookingRepository2.findById(tripId).get();
		tripBooking.setTripStatus(TripStatus.CANCELED);
		tripBooking.setBill(0);
		tripBookingRepository2.save(tripBooking);

		tripBooking.getDriver().getCab().setAvailable(true);
		// making the cab of driver of this cancelled trip as available

	}

	@Override
	public void completeTrip(Integer tripId)
	{
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripBooking = tripBookingRepository2.findById(tripId).get();
		tripBooking.setTripStatus(TripStatus.COMPLETED);

		int distance = tripBooking.getDistanceInKm();

		Cab cab = tripBooking.getDriver().getCab();

		int rate = cab.getPerKmRate();

		tripBooking.setBill(distance * rate);
		tripBookingRepository2.save(tripBooking);

		tripBooking.getDriver().getCab().setAvailable(true);
		// making the cab of driver of this completed trip as available
	}
}
