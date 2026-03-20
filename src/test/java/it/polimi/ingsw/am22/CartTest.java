package it.polimi.ingsw.am22;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @BeforeAll
    void setUp() {
        cart = new Cart();
    }

    @Test
    void testShouldAddToCart(){
        Cart cart = new Cart();
        //0
        assertEquals(0.0, cart.getTotal());
        cart.addElement("mouse",50);
        //50
        assertEquals(50.0, cart.getTotal());
    }

    @Test
    void testShouldObtainDiscount(){
        Cart cart = new Cart();
        assertEquals(0.0, cart.getTotal());
        cart.addElement("mouse",50);
        cart.addElement("mouse",50);
        cart.addElement("mouse",50);
        cart.addElement("mouse",50);
        assertEquals(180.0, cart.getTotal());
    }

    @Test
    void testShouldNotAddNegativePrice(){
        assertThrows(
                IllegalArgumentException.class,
                () -> Cart.addElement())
    }
}