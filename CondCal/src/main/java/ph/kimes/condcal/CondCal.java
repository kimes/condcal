package ph.kimes.condcal;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CondCal {

    private static final String REGEX_QUOTATIONS =
            "(\\(\\([a-zA-Z0-9=<>&()]{1,}\\)=\\([a-zA-Z0-9.+\\-*/]{1,}\\)\\))(>>|){1,}",
        REGEX_CONDITION_VALUE = "\\(\\((.{1,})\\)=\\((.{1,})\\)\\)",
        REGEX_CONDITION = "((Date|Number|)(\\(|)([\\w-]{1,})(==|!=|>|>=|<|<=)([\\w-]{1,})(\\)|))(&&|\\|\\||){1,}";

    private static final String REGEX_HAS_PARENTHESIS = "\\((.{1,})\\)",
        REGEX_PARENTHESIS = "\\((((-|)\\d{1,}((?=\\.)\\.\\d{1,}|)((?=[+\\-*/])[+\\-*/]{1})){1,}(-|)\\d{1,}((?=\\.)\\.\\d{1,}|))\\)",
        REGEX_MUL_DIV = "(((?=[+\\-*/])[+\\-]{0,}|(?=^)[+\\-]{0,})\\d+(\\.(?=\\d)|)\\d{0,})([*/]{1})(((?=[+\\-*/])[+\\-]{0,}|)\\d+(\\.(?=\\d)|)\\d{0,})",
        REGEX_ADD_SUB = "(((?=[+\\-*/])[+\\-]{0,}|(?=^)[+\\-]{0,})\\d+(\\.(?=\\d)|)\\d{0,})([+\\-]{1})(((?=[+\\-*/])[+\\-]{0,}|)\\d+(\\.(?=\\d)|)\\d{0,})";

    private String condCal = "";
    private JSONObject parameters = new JSONObject();

    public CondCal() {}
    public CondCal(String condCal, JSONObject parameters) {
        this.condCal = condCal;
        this.parameters = parameters;
    }

    public double getValue() throws CondCalException {
        double v = 0d;
        Pattern pattern = Pattern.compile(REGEX_QUOTATIONS);
        Matcher matcher = pattern.matcher(condCal);
        while (matcher.find()) {
            String conditionValue = matcher.group(1);

            Pattern patternConditionValue = Pattern.compile(REGEX_CONDITION_VALUE);
            Matcher matcherConditionValue = patternConditionValue.matcher(conditionValue);
            if (matcherConditionValue.matches()) {
                String condition = matcherConditionValue.group(1),
                        value = matcherConditionValue.group(2);

                System.out.println("CHECKING: " + condition);
                if (checkCondition(condition, parameters)) {
                    try {
                        v = getValue(value, parameters);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    System.out.println("THIS IS IT: " + v);
                    break;
                }
            }
        }

        return 0;
    }

    private boolean checkCondition(String condition, JSONObject params) throws CondCalException {
        ArrayList<Boolean> allGrants = new ArrayList<>();

        Pattern pattern = Pattern.compile(REGEX_CONDITION);
        Matcher matcher = pattern.matcher(condition);
        while (matcher.find()) {
            boolean granted = false;
            String paramName1 = matcher.group(4),
                paramName2 = matcher.group(6), operation = matcher.group(5);
            String type = "Text";
            if (!matcher.group(2).isEmpty()) type = matcher.group(2);

            String param1 = "", param2 = "";
            try {
                if (params.has(paramName1)) param1 = params.getString(paramName1);
                if (params.has(paramName2)) param2 = params.getString(paramName2);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("CHECKING CONDITIONS | " + type + ": " + param1 + " == " + param2);
            if (type.equals("Text")) {
                if (operation.equals("==")) {
                    if (param1.equals(param2)) {
                        granted = true;
                    }
                }
            } else if (type.equals("Number")) {
                double paramDouble1 = 0d, paramDouble2 = 0d;
                try {
                    paramDouble1 = Double.parseDouble(param1);
                    paramDouble2 = Double.parseDouble(param2);
                } catch (NumberFormatException e) {
                    throw new CondCalException(e.getMessage());
                }

                if (operation.equals("==")) {
                    if (paramDouble1 == paramDouble2) {
                        granted = true;
                    }
                } else if (operation.equals(">")) {
                    if (paramDouble1 > paramDouble2) {
                        granted = true;
                    }
                } else if (operation.equals(">=")) {
                    if (paramDouble1 >= paramDouble2) {
                        granted = true;
                    }
                } else if (operation.equals("<")) {
                    if (paramDouble1 < paramDouble2) {
                        granted = true;
                    }
                } else if (operation.equals("<=")) {
                    if (paramDouble1 <= paramDouble2) {
                        granted = true;
                    }
                }
            } else if (type.equals("Date")) {
                Date paramDate1 = null, paramDate2 = null;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    paramDate1 = dateFormat.parse(param1);
                    paramDate2 = dateFormat.parse(param2);
                } catch (ParseException e) {
                    throw new CondCalException(e.getMessage());
                }

                if (operation.equals("==")) {
                    if (paramDate1.compareTo(paramDate2) == 0) {
                        granted = true;
                    }
                } else if (operation.equals(">")) {
                    if (paramDate1.after(paramDate2)) {
                        granted = true;
                    }
                } else if (operation.equals(">=")) {
                    if (paramDate1.compareTo(paramDate2) == 0) {
                        granted = true;
                    }
                    if (paramDate1.after(paramDate2)) {
                        granted = true;
                    }
                } else if (operation.equals("<")) {
                    if (paramDate1.before(paramDate2)) {
                        granted = true;
                    }
                } else if (operation.equals("<=")) {
                    if (paramDate1.compareTo(paramDate2) == 0) {
                        granted = true;
                    }
                    if (paramDate1.before(paramDate2)) {
                        granted = true;
                    }
                }
            }

            allGrants.add(granted);
        }

        boolean allGranted = true;
        for (int i = 0; i < allGrants.size(); i++) {
            if (!allGrants.get(i)) allGranted = false;
        }
        return allGranted;
    }

    private double getValue(String formula, JSONObject params) {
        String nFormula = formula;

        Pattern pattern = Pattern.compile("[a-zA-Z]{1,}");
        Matcher matcher = pattern.matcher(nFormula);

        // CONVERTING VARIABLES TO NUMBERS
        ArrayList<MatchResult> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.toMatchResult());
        }
        for (int i = matches.size() - 1; i >= 0; i--) {
            MatchResult iMatch = matches.get(i);

            double iValue = 0d;
            try {
                if (params.has(iMatch.group())) iValue = params.getDouble(iMatch.group());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            nFormula = nFormula.substring(0, iMatch.start()) +
                    nFormula.substring(iMatch.start() + iMatch.group().length());
            nFormula = nFormula.substring(0, iMatch.start()) + iValue + nFormula.substring(iMatch.start());
        }

        System.out.println("FORMULA: " + nFormula + " = " + getValueInParenthesis(nFormula));
        return getValueInParenthesis(nFormula);
    }

    private double getValueInParenthesis(String formula) {
        String nFormula = formula;

        Pattern patternParenthesis = Pattern.compile(REGEX_PARENTHESIS),
                patternMulDiv = Pattern.compile(REGEX_MUL_DIV),
                patternAddSub = Pattern.compile(REGEX_ADD_SUB);

        // PARENTHESIS
        while (Pattern.matches(REGEX_HAS_PARENTHESIS, nFormula)) {
            ArrayList<MatchResult> matches = new ArrayList<>();

            Matcher matcher = patternParenthesis.matcher(nFormula);
            while (matcher.find()) {
                matches.add(matcher.toMatchResult());
            }
            for (int i = matches.size() - 1; i >= 0; i--) {
                MatchResult iMatch = matches.get(i);

                double iValue = getValueInParenthesis(nFormula);

                nFormula = nFormula.substring(0, iMatch.start()) +
                        nFormula.substring(iMatch.start() + iMatch.group().length());
                nFormula = nFormula.substring(0, iMatch.start()) + iValue + nFormula.substring(iMatch.start());
            }
        }

        double iValue = 0d;
        String concat = "";
        // MULTIPLICATION & DIVISION
        Matcher matcherMulDiv = patternMulDiv.matcher(nFormula);
        while (matcherMulDiv.find()) {
            double param1 = 0d, param2 = 0d;
            try {
                param1 = Double.parseDouble(matcherMulDiv.group(1));
                param2 = Double.parseDouble(matcherMulDiv.group(5));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            if (matcherMulDiv.group(4).equals("*")) {
                iValue = param1 * param2;
            } else if (matcherMulDiv.group(4).equals("/")) {
                iValue = param1 / param2;
            }

            concat = (iValue >= 0) ? "+" + iValue : iValue + "";

            nFormula = nFormula.substring(0, matcherMulDiv.start()) +
                    nFormula.substring(matcherMulDiv.start() + matcherMulDiv.group(0).length());
            nFormula = nFormula.substring(0, matcherMulDiv.start()) + concat +
                    nFormula.substring(matcherMulDiv.start());

            matcherMulDiv = patternMulDiv.matcher(nFormula);
        }

        // ADDITION & SUBTRACTION
        Matcher matcherAddSub = patternAddSub.matcher(nFormula);
        while (matcherAddSub.find()) {
            double param1 = 0d, param2 = 0d;
            try {
                param1 = Double.parseDouble(matcherAddSub.group(1));
                param2 = Double.parseDouble(matcherAddSub.group(5));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            if (matcherAddSub.group(4).equals("+")) {
                iValue = param1 + param2;
            } else if (matcherAddSub.group(4).equals("-")) {
                iValue = param1 - param2;
            }

            concat = (iValue >= 0) ? "+" + iValue : iValue + "";

            nFormula = nFormula.substring(0, matcherAddSub.start()) +
                    nFormula.substring(matcherAddSub.start() + matcherAddSub.group(0).length());
            nFormula = nFormula.substring(0, matcherAddSub.start()) + concat +
                    nFormula.substring(matcherAddSub.start());

            matcherAddSub = patternAddSub.matcher(nFormula);
        }

        double value = 0d;
        try {
            value = Double.parseDouble(nFormula);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return value;
    }

    public String getCondCal() { return condCal; }
    public JSONObject getParameters() { return parameters; }

    public void setCondCal(String condCal) { this.condCal = condCal; }
    public void setParameters(JSONObject parameters) { this.parameters = parameters; }
}
