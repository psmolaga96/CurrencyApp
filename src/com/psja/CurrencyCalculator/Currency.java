package com.psja.CurrencyCalculator;

import java.awt.Toolkit;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class Currency extends Thread {
    private CurrencyCalculator frame;
    protected ArrayList<CurrencyData> currencyList = new ArrayList<>();
    private String date ="";
    
    public Currency(CurrencyCalculator frame){
        this.frame = frame;
    }
    protected ArrayList<CurrencyData> getArrayDataList(){
        return currencyList;
    }
    protected static class CurrencyData {
        private String currency;
        private String code;
        private double mid;

        public CurrencyData(String currency, String code, double mid) {
            this.currency = currency;
            this.code = code;
            this.mid = mid;
        }

        public String getCurrency() {
            return currency;
        }

        public String getCode() {
            return code;
        }

        public double getMid() {
            return mid;
        }
    }
    public String getDate(){
        return date;
    }
    public void CurrencyJComboboxUpdater(ArrayList<CurrencyData> currencyDataList){
        frame.jComboCurrencyList.removeAllItems();
        for (CurrencyData currencyData : currencyDataList) {
            frame.jComboCurrencyList.addItem(currencyData.getCurrency());
        }
    }
    public void CurrencyTableUpdater(ArrayList<CurrencyData> currencyDataList){
        DefaultTableModel model = (DefaultTableModel) frame.jTab.getModel();
        model.setRowCount(0);
        
        for (CurrencyData currencyData : currencyDataList) {
            Object[] rowData = {currencyData.getCurrency(), currencyData.getCode(), currencyData.getMid()};
            model.addRow(rowData);
        }
        frame.jDate.setText(frame.jDate.getText()+getDate());
        frame.jDate1.setText(frame.jDate.getText());
    }
    private ArrayList<CurrencyData> convertToList(String response){
        ArrayList<CurrencyData> currencyDataList = new ArrayList<>();
        int startIndex = response.indexOf("[");
        int endIndex = response.indexOf("]");
            
        String currencyTable1 = response.substring(startIndex+1, endIndex+1);
        startIndex = currencyTable1.indexOf("[");
        endIndex = currencyTable1.indexOf("]");
        String currencyTable = currencyTable1.substring(startIndex+2, endIndex-1);
        String[] currencyArray = currencyTable.split("\\},\\{");
        currencyDataList.add(new CurrencyData("złoty","PLN",1.));

        for (String currency : currencyArray) {
            currency = currency.replace("[", "").replace("]", "");
            String[] keyValuePairs = currency.split(",");
            String code = "";
            String name = "";
            double mid = 0.0;
            
            for (String pair : keyValuePairs) {
                String[] parts = pair.split(":");
                String key = parts[0].replaceAll("\"", "").trim();
                String value = parts[1].replaceAll("\"", "").trim();

                if (key.equals("code")) {
                    code = value;
                } else if (key.equals("currency")) {
                    name = value;
                } else if (key.equals("mid")) {
                    mid = Double.parseDouble(value);
                }
            }
            currencyDataList.add(new CurrencyData(name,code,mid));
        }
        return currencyDataList;
    }
    private String getServerData() {
	try {
	    Socket soc = new Socket("api.nbp.pl",80);
            OutputStream outputStream = soc.getOutputStream();
	     BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
             outputStream.write(("GET /api/exchangerates/tables/A HTTP/1.1\r\n" +
                    "Host: api.nbp.pl\r\n" +
                    "Connection: close\r\n\r\n").getBytes());
            outputStream.flush();
            StringBuilder responseBuilder = new StringBuilder();
	    String str;
            while ((str = reader.readLine()) != null) {
                responseBuilder.append(str);
            }
            outputStream.close();
            reader.close();
	    soc.close();
            
            String response = responseBuilder.toString(); //strona w string
            
            int dateStartIndex = response.indexOf("effectiveDate");
            int dateEndIndex = response.indexOf("rates");
            date = response.substring(dateStartIndex+16, dateEndIndex-3);
            return response;
	} catch (IOException ex) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(null, "Wystąpił błąd podczas próby połączenia się z serwerem", "Błąd połączenia", JOptionPane.INFORMATION_MESSAGE);
            System.exit(1);
            return null;
        }
    }
    
    public void calculate(){
        double valuePLN=0;
        try{
            if(Double.parseDouble(frame.jamount.getText().replaceAll(",", "."))<=0){
                throw new NumberFormatException();
            }
            double selectedRate=0;
            for(Currency.CurrencyData data:currencyList){
                if(frame.jComboCurrencyList.getSelectedItem().toString().equals(data.getCurrency())){
                    selectedRate=data.getMid();
                    valuePLN = Double.parseDouble(frame.jamount.getText().replaceAll(",", ".")) * data.getMid();
                }
            }
            DefaultTableModel model = (DefaultTableModel) frame.jCurrencyToPLN.getModel();
            model.setRowCount(0);
            double otherCurrencyValue=0;
            for (Currency.CurrencyData currencyData : currencyList) {
                if (!frame.jComboCurrencyList.getSelectedItem().toString().equals(currencyData.getCurrency())) {
                    otherCurrencyValue=((Double.parseDouble(frame.jamount.getText().replaceAll(",", "."))*selectedRate)/currencyData.getMid());
                    Object[] rowData = {currencyData.getCurrency(),currencyData.getCode(),String.format("%.6f",otherCurrencyValue)};
                    model.addRow(rowData); 
                }
            }
        
        }
        catch (NumberFormatException e){
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(null, "Wprowadź poprawną watorść", "Niepoprawna watorść", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }
    
    @Override
	public void run() {
            currencyList = convertToList(getServerData());
            CurrencyTableUpdater(currencyList);
            CurrencyJComboboxUpdater(currencyList);
            
	}
}
