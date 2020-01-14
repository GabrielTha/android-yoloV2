package org.tensorflow.yolo.view;

import org.tensorflow.yolo.model.Recognition;

import java.util.List;

public class VerifyClassification {
    public boolean verifyFossa(List<Recognition> lista){
        Boolean flag = false;
        if(lista.isEmpty()){
            return(false);
        }
        for (int i = 0; i < lista.size(); i++){
            Recognition reconhecido;
            reconhecido = lista.get(i);
            if (reconhecido.getTitle().equals("fossa")){
                if (flag == false){
                    flag = true;
                }
                else{
                    return(true);
                }
            }
        }
        return(false);
    }

    public boolean verifyBoca(List<Recognition> lista){
        if(lista.isEmpty()){
            return(false);
        }
        for (int i = 0; i < lista.size(); i++){
            Recognition reconhecido;
            reconhecido = lista.get(i);
            if (reconhecido.getTitle().equals("boca")){
                return(true);
            }
        }
        return(false);
    }

    public String verifyList(List<Recognition> lista){
        if(this.verifyBoca(lista) && this.verifyFossa(lista)){
            return("Achou");
        }
        return ("Não achou");
    }
}
