package com.vending.controller;

import com.vending.entity.Coin;
import com.vending.entity.Product;
import com.vending.repository.CoinRepository;
import com.vending.repository.MachineRepository;
import com.vending.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

@RestController
public class PurchaseController {

    private final ProductRepository productRepository;
    private final MachineRepository machineRepository;
    private final CoinRepository coinRepository;

    /**
     * Product Rest Controller  constructor
     *
     * @param productRepository the repository for all the products
     * @param machineRepository the repository for all the machines
     */
    @Autowired
    PurchaseController(ProductRepository productRepository,
                          MachineRepository machineRepository, CoinRepository coinRepository) {
        this.productRepository = productRepository;
        this.machineRepository = machineRepository;
        this.coinRepository =coinRepository;

    }


    /**
     * Buy a specific item from a specific machine
     *
     * @param machineId the vending machine
     * @param productId the product
     * @return the product you have purchased
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{machineId}/products/{productId}/buy")
    Product  buyProduct(@PathVariable String machineId, @PathVariable Long productId, @RequestBody List<Coin> coins) {
        this.validateMachine(machineId);

        coins.forEach(coin -> {
             this.machineRepository
                    .findByName(machineId)
                    .map(machine -> {
                        final boolean[] coinFound = {false};

                        // Increase the amount of coins if the machine already has seen that coin
                        this.coinRepository.findByMachineName(machineId).forEach(machineCoin -> {
                            if (coin.value == machineCoin.value) {
                                coin.amount++;
                                coinFound[0] = true;
                            }
                        });

                        // Else add the coin to the repository
                        if (!coinFound[0] && IntStream.of(coin.POSSIBLE_VALUES).anyMatch(x -> x == coin.value)) {
                            this.coinRepository.saveAndFlush(new Coin(machine, coin.value, 1));
                            coinFound[0] = true;
                        }

                        if (coinFound[0]) {
                            machine.currentAmount += coin.value;
                        }

                        this.machineRepository.saveAndFlush(machine);

                        return this.machineRepository.findByName(machineId);
                    });
        });

        final Product[] soldProduct = new Product[1];

        this.machineRepository.findByName(machineId).map(machine -> {

            this.productRepository.findByMachineName(machineId)
                    .forEach(product -> {
                        if (product.getId() == productId
                                && product.cost <= machine.currentAmount
                                && product.quantity > 0) {
                            product.quantity--;
                            this.productRepository.saveAndFlush(product);
                            machine.currentAmount -= product.cost;
                            product.quantity = 1;
                            soldProduct[0] = product;
                        }
                    });

            this.machineRepository.saveAndFlush(machine);
            return machine;
        });
        return soldProduct[0];
    }

    /**
     * Validate that a given vending machine exists
     *
     * @param machineId the machine name.
     */
    private void validateMachine(String machineId) {
        this.machineRepository.findByName(machineId).orElseThrow(
                () -> new MachineNotFoundException(machineId));
    }

}
