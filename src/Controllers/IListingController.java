package Controllers;

import java.io.IOException;

public interface IListingController
{
  String getProductCategories() throws IOException;
  String getProducts() throws IOException;
  String getListing(String id) throws IOException;
  String createListing(String str) throws IOException;
}