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

public class Option {
    
    private final int params;
    private final boolean mandatory;
    private boolean founded;
    private String value;
    private final String[] sentences;
    
    public Option(int params, boolean mandatory, String value, String ... sentences) {
        this.params = params;
        this.mandatory = mandatory;
        this.founded = false;
        this.value = value;
        this.sentences = sentences;
    }

    public int getParams() {
        return this.params;
    }
    
    public String getValue() {
        return this.value;
    }
    
    public boolean isMandatory() {
        return this.mandatory;
    }
    
    public boolean isFounded() {
        return this.founded;
    }
    
    public void founded() {
        this.founded = true;
    }
    
    public String generateOption(String option) {
        if ("".equals(this.value)) {        
            if (this.isMandatory())            
                return " " + option + "";
            else
                return " [" + option + "]";        
        }
        else {
            if (this.isMandatory())            
                return " " + option + " " + this.value;
            else
                return " [" + option + " " + this.value +"]";  
        }
    }
    
    public void print(String option) {
        System.out.println("        " + option + "   " + this.sentences[0]);        
        for (int i = 1; i < this.sentences.length; i++) {
            System.out.println("             " + this.sentences[i]);
        }
        System.out.println("");
    }
}
