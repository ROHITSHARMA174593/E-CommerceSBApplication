package com.ecom.EcomSB.service;

import com.ecom.EcomSB.model.User;
import com.ecom.EcomSB.payload.AddressDTO;
import org.springframework.lang.NonNull;

import java.util.List;

public interface AddressService {
    AddressDTO createAddress(AddressDTO addressDTO, User user);

    List<AddressDTO> getAddresses();

    AddressDTO getAddressById(@NonNull Long addressId);

    List<AddressDTO> getUserAddresses(User user);

    AddressDTO updateAddress(@NonNull Long addressId, AddressDTO addressDTO);

    String deleteAddressById(@NonNull Long addressId);
}
