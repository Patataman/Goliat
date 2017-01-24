/************************************************************************
 * Planning and Learning Group PLG,
 * Department of Computer Science,
 * Carlos III de Madrid University, Madrid, Spain
 * http://plg.inf.uc3m.es
 * 
 * Copyright 2015, Moises Martinez
 *
 * (Questions/bug reports now to be sent to Moisés Martínez)
 *
 * This file is part of IAIE.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the IAIE nor the names of its contributors may be 
 *       used to endorse or promote products derived from this software without 
 *       specific prior written permission.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with IAIE. If not, see <http://www.gnu.org/licenses/>.
 * 
 ************************************************************************/

package org.iaie.tools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.iaie.exception.BadParametersException;

public class Options {
    
    private static Options instance = new Options();
    
    public static Options getInstance() {
	return instance;
    }
    
    private final HashMap<String, Option> paramTree;
    private final HashMap<String, String> options;
    private String optionsAsString;
    private final String[] order;
    
    private Options() {
        this.options = new HashMap<>();
        this.optionsAsString = "";
        
        this.paramTree = new HashMap<>();
        this.paramTree.put("-a", new Option(2, true, "agente", new String[]{"Establece el agente que será ejecutado para jugar a StarCraft.", "El usuario deberá introducir la ruta completa del agente con", "respecto a la distribución de paquetes (sin incluir la extensión)"}));
        this.paramTree.put("-s", new Option(2, false, "velocidad", new String[]{"Establece la velocidad de ejecución de la partida (0 - 10)."}));
        this.paramTree.put("-i", new Option(1, false, "", new String[]{"Establece la inyección de información por parte del jugador."}));
        this.paramTree.put("-u", new Option(1, false, "", new String[]{"Establece la recepción de información perfecta del entorno."}));
        //this.paramTree.put("-f", new Option(2, false, "fichero", new String[]{"Establece el fichero en el que se encuentran las coordenadas."}));
        //this.paramTree.put("-t", new Option(2, false, "tipo", new String[]{"Establece el tipo de búsqueda a realizar."}));
        //this.paramTree.put("-p", new Option(2, false, "paquete", new String[]{"Establece el nombre del paquete en el cual se encuentran almanenados", "algoritmos de búsqueda."}));
        this.paramTree.put("-d", new Option(1, false, "", new String[]{"(debug mode) Permite visualizar el proceso de búsqueda."}));

        //this.order = new String[]{"-a", "-s", "-i", "-u", "-f", "-t", "-p", "-d"};
        this.order = new String[]{"-a", "-s", "-i", "-u", "-d"};
    }
    
    public void readOptions(String[] args) throws Exception {
        
        int position = 0;
        
        if (args != null) {
            if (args.length > 0) {
                while(position < args.length) {
                    
                    Option op = this.paramTree.get(args[position]);
                    
                    if (op == null) 
                        throw new NullPointerException();
                    else {
                        if (op.getParams() == 2) {
                            if (args[position+1].contains("-")) {
                                throw new BadParametersException();
                            }
                            else {
                                this.optionsAsString += args[position] + " " + args[position+1] + " ";
                                this.options.put(args[position].trim(), args[position+1].trim());
                            }
                            position+=2;
                        }
                        else {
                            this.optionsAsString += args[position] + " ";
                            this.options.put(args[position].trim(), "true");
                            position++;
                        }
                        op.founded();
                    }
                }
            }
            else {
                throw new Exception();
            }
        }
        else {
            throw new NullPointerException();
        }
        
        Iterator element = this.paramTree.entrySet().iterator();
        
        while (element.hasNext()) {
            
            Option op = (Option) ((Map.Entry) element.next()).getValue();
            
            if (op.isMandatory() && !op.isFounded()) 
                throw new Exception();
        }
    }
    
    public String getOption(String key) {
        return this.options.get(key);
    }
    
    public String getAgent() {
        return this.options.get("-a");
    }
    
    public String asString() {
        return this.optionsAsString;
    }
    
    public int getSpeed() {
        String op = this.options.get("-s");
        if (op != null)
            return Integer.parseInt(op);
        else 
            return 1;
    }
    
    public boolean getUserInput() {
        String op = this.options.get("-u");
        System.out.println("USER INPUT: " + op);
        if (op != null)
            return Boolean.parseBoolean(op);
        else
            return false;
    }
    
    public boolean getInformation() {
        String op = this.options.get("-i");
        System.out.println("INFORMATION: " + op);
        if (op != null)
            return Boolean.parseBoolean(op);
        else
            return false;
    }   
    
    public String getFile() {
        return this.options.get("-f");
    }
    
    public int getSearchType() {
        return Integer.parseInt(this.options.get("-t"));
    }
    
    public String getPacketName() {
        return this.options.get("-p");
    }
    
    public boolean isDebug() {
        return Boolean.parseBoolean(this.options.get("-d"));
    }
    
    public void printOptions() {
        
        System.out.println("NOMBRE");
        System.out.println("        java -jar program -- Ejecuta el jugador de StarCraft");
        System.out.println();
        System.out.println("SINOPSIS");
        System.out.print("        java -jar program");
        
        for (String o : this.order) {
            System.out.print(this.paramTree.get(o).generateOption(o));
        }

        System.out.println();        
        System.out.println();
        System.out.println("DESCRIPCION");
        System.out.println("        Este programa debe ser ejecutado utilizando JAVA. Para ello se");
        System.out.println("        deberá ejecutar el programa compilado cuyo nombre sustiuriá a ");
        System.out.println("        program.");
        System.out.println("");
        System.out.println("        Las siguiente opciones están disponibles:");
        System.out.println("");
        
        for (String o : this.order) {
            this.paramTree.get(o).print(o);
        }
    }         
}